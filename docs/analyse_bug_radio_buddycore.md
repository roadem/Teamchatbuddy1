# Analyse et Correction du Bug : Radio qui Démarre dans BuddyCore

**Application :** TeamChatBuddy
**Plateforme :** Robot Buddy (Android)
**Date :** 2026-03-13
**Statut :** Corrigé

---

## Table des matières

1. [Contexte](#1-contexte)
2. [Description du bug](#2-description-du-bug)
3. [Analyse des causes racines](#3-analyse-des-causes-racines)
4. [Preuves dans les logs](#4-preuves-dans-les-logs)
5. [Les trois corrections appliquées](#5-les-trois-corrections-appliquées)
6. [Flux corrigé complet](#6-flux-corrigé-complet)
7. [Résumé](#7-résumé)

---

## 1. Contexte

L'application TeamChatBuddy s'exécute sur un robot Buddy. Lorsque l'utilisateur dit **"Lance la radio RTL"**, l'application effectue la séquence suivante :

1. `CMD_RADIO()` est appelée sur le thread principal (main thread).
2. Un thread en arrière-plan est lancé pour appeler les APIs dans l'ordre :
   - obtention du token d'authentification
   - obtention du nom de la radio
   - obtention de l'URL du flux audio
3. Une fois l'URL obtenue, `playRadio(url)` est appelée depuis ce thread d'arrière-plan.
4. `playRadio()` crée un `MediaPlayer`, positionne `PlayingRadio = true`, puis lance `prepareAsync()` (chargement asynchrone du flux).
5. Un callback `onPreparedListener` est enregistré : quand le `MediaPlayer` est prêt, il appelle `radioPlayer.start()`.

Par ailleurs, le robot Buddy dispose d'un menu système appelé **BuddyCore** :
- Quand l'utilisateur **ouvre** le menu BuddyCore → `onPause()` est déclenché dans l'activité principale.
- Quand l'utilisateur **ferme** le menu BuddyCore → `onResume()` est déclenché.

---

## 2. Description du bug

**Titre :** La radio démarre alors que le menu BuddyCore est ouvert.

**Comportement observé :** L'utilisateur demande de lancer la radio, puis ouvre le menu BuddyCore pendant que l'application charge le flux audio. Malgré l'application étant en pause (`onPause()` déclenché), la radio commence à jouer en arrière-plan — alors qu'elle ne devrait démarrer qu'au retour dans l'application (`onResume()`).

### Code original impliqué

**`CMD_RADIO()` — thread d'arrière-plan (ligne ~4823)**

```java
// Dans CMD_RADIO() — le thread d'arrière-plan appelle directement playRadio()
new Thread(() -> {
    // ... appels API ...
    String url = ...; // URL du flux radio
    playRadio(url);   // appelé depuis le thread d'arrière-plan — BUG ICI
}).start();
```

**`playRadio()` — sans vérification de l'état de l'application (ligne ~7115)**

```java
private void playRadio(String radioUrl){
    radioPlayer = new MediaPlayer();
    teamChatBuddyApplication.setPlayingRadio(true);
    radioPlayer.prepareAsync();

    radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            radioPlayer.start(); // aucune vérification isOnApp — BUG ICI
        }
    });
}
```

**`onResume()` — appel de `start()` sans protection (ligne ~963)**

```java
if(teamChatBuddyApplication.isPlayingRadio()){
    if (commande.radioPlayer != null) {
        commande.radioPlayer.start(); // aucune protection — BUG ICI
    }
}
```

---

## 3. Analyse des causes racines

Le bug découle de **trois causes indépendantes mais combinées**.

---

### Cause racine 1 : `MediaPlayer` créé sur un thread sans Looper

Sur Android, `MediaPlayer` doit être créé sur un thread possédant un **Looper** — typiquement le thread principal (main thread). Le Looper est la boucle de messages qui permet à Android de dispatcher les événements internes du `MediaPlayer` (dont `onPrepared`).

**Ce qui se passe sans Looper :**

```
Thread d'arrière-plan (sans Looper)
         │
         ├── new MediaPlayer()              ← création sans Looper
         ├── prepareAsync()                 ← lancement asynchrone
         └── setOnPreparedListener(...)     ← callback enregistré

         Quand le flux est prêt :
         → Android cherche un Looper pour dispatcher onPrepared()
         → Comportement imprévisible / non garanti
         → onPrepared() peut s'exécuter sur n'importe quel thread
         → La radio peut démarrer à tout moment, même pendant onPause()
```

**Conséquence directe :** Dans certaines conditions, `onPrepared()` se déclenche **pendant que BuddyCore est ouvert** (application en pause) et appelle `radioPlayer.start()` sans aucune vérification préalable.

---

### Cause racine 2 : Absence de vérification `isOnApp` dans `onPrepared`

Même si le `MediaPlayer` était correctement créé sur le thread principal, le callback `onPrepared` appelait `radioPlayer.start()` **sans vérifier si l'application était au premier plan**.

La variable `isOnApp` de `TeamChatBuddyApplication` représente cet état :
- `isOnApp = false` → l'application est en pause (BuddyCore ouvert, ou autre)
- `isOnApp = true` → l'application est au premier plan

```
Cycle de vie Android avec BuddyCore
─────────────────────────────────────────────────────────────────────────
onPause()   → isOnApp = false   ← BuddyCore s'ouvre
onResume()  → init() appelé     ← isOnApp = true (dans init())
─────────────────────────────────────────────────────────────────────────

Code original — onPrepared SANS vérification :

onPrepared() {
    radioPlayer.start();   // démarre QUOI QU'IL ARRIVE
                           // même si isOnApp == false
}
```

---

### Cause racine 3 : `onResume()` appelle `start()` sans vérifier l'état du `MediaPlayer`

Le `MediaPlayer` Android suit une **machine à états stricte**. Appeler `start()` alors que le `MediaPlayer` est encore dans l'état `PREPARING` (le chargement du flux n'est pas terminé) lève une `IllegalStateException`.

```
États possibles du MediaPlayer
─────────────────────────────────────────────────────────────────────────
IDLE → INITIALIZED → PREPARING → PREPARED → STARTED → PAUSED
                         ↑
                    prepareAsync() en cours
                    (peut durer 2-3 secondes)
─────────────────────────────────────────────────────────────────────────

Scénario problématique dans onResume() :

1. L'utilisateur ouvre BuddyCore pendant que prepareAsync() tourne
2. Le flux se charge pendant que BuddyCore est ouvert (PREPARING)
3. L'utilisateur ferme BuddyCore → onResume() appelé
4. isPlayingRadio == true → onResume() appelle radioPlayer.start()
5. MAIS : MediaPlayer est encore en PREPARING
   → IllegalStateException !
```

---

## 4. Preuves dans les logs

### Avant la correction — comportement anormal

Aucun log `RADIO_DEBUG` n'apparaissait (le callback `onPrepared` n'était pas fiable). La radio démarrait toutefois car `onResume()` appelait `start()` directement. Dans les cas limites où `onPrepared` se déclenchait pendant la pause, la radio démarrait dans BuddyCore.

### Après la correction — comportement correct

```
10:29:39  onPause()     → isOnApp=false          ← BuddyCore ouvert
10:29:43  playRadio()   → isOnApp=false           ← chargement radio pendant BuddyCore
10:29:43  prepareAsync() lancé
10:29:45  onPrepared()  → isOnApp=false           ← flux prêt, mais app en pause
                        → start() ignoré ✅        ← la radio ne démarre PAS dans BuddyCore
10:30:21  onResume()    → radioPlayer.start() ✅   ← radio démarre quand BuddyCore est fermé
10:30:28  onPause()     → radioPlayer.pause() ✅   ← radio mise en pause si BuddyCore rouvre
                        → isPlaying=true
```

La séquence est désormais parfaitement cohérente avec le cycle de vie attendu.

---

## 5. Les trois corrections appliquées

---

### Correction 1 : `playRadio()` posté sur le thread principal

**Fichier :** `Commande.java` — ligne ~4877-4879

**Avant :**
```java
playRadio(url);
```

**Après :**
```java
String finalUrl = url;
new Handler(Looper.getMainLooper()).post(() -> playRadio(finalUrl));
```

**Explication :**

`Handler(Looper.getMainLooper()).post(...)` envoie la tâche dans la file de messages du thread principal. Le `MediaPlayer` est ainsi créé sur le main thread, qui possède un Looper. Android peut alors dispatcher `onPrepared` de manière fiable sur ce même thread.

```
Thread d'arrière-plan
         │
         ├── appels API (token, nom, URL)
         │
         └── Handler(Looper.getMainLooper()).post(() -> playRadio(url))
                                                         │
                          ┌──────────────────────────────┘
                          │        [Main Thread — avec Looper]
                          ├── new MediaPlayer()       ← créé proprement
                          ├── prepareAsync()
                          └── setOnPreparedListener() ← callback fiable
                                       │
                                 onPrepared() garanti
                                 sur le main thread
```

---

### Correction 2 : Vérification `isOnApp` dans `onPrepared`

**Fichier :** `Commande.java` — ligne ~7133-7143

**Avant :**
```java
radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
    @Override
    public void onPrepared(MediaPlayer mp) {
        radioPlayer.start();
    }
});
```

**Après :**
```java
radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("RADIO_DEBUG", "onPrepared() → isOnApp=" + teamChatBuddyApplication.isOnApp);
        if (teamChatBuddyApplication.isOnApp) {
            Log.i("RADIO_DEBUG", "onPrepared() → start() appelé");
            radioPlayer.start();
        } else {
            Log.w("RADIO_DEBUG", "onPrepared() → app en pause, start() ignoré");
        }
    }
});
```

**Explication :**

Même dans le cas où `onPrepared` se déclenche correctement, cette garde (`isOnApp`) empêche le démarrage si l'application est en pause. La responsabilité de démarrer la radio est alors **délégée à `onResume()`**, qui sera appelé quand l'utilisateur fermera BuddyCore.

```
onPrepared() déclenché
        │
        ├── isOnApp == true  → radioPlayer.start()  ← démarrage normal
        │
        └── isOnApp == false → start() IGNORÉ       ← app en pause
                               onResume() démarrera
                               la radio plus tard
```

---

### Correction 3 : `start()` protégé dans `onResume()`

**Fichier :** `MainFragment.java` — ligne ~963-980

**Avant :**
```java
if(teamChatBuddyApplication.isPlayingRadio()){
    if (commande.radioPlayer != null) {
        commande.radioPlayer.start();
    }
}
```

**Après :**
```java
if(teamChatBuddyApplication.isPlayingRadio()){
    if (commande.radioPlayer != null) {
        try {
            if (!commande.radioPlayer.isPlaying()) {
                commande.radioPlayer.start();
            }
        } catch (IllegalStateException e) {
            Log.w("RADIO_DEBUG", "onResume() → MediaPlayer pas encore prêt, onPrepared lancera start()");
        }
    }
}
```

**Explication :**

Deux protections ont été ajoutées :

| Protection | Rôle |
|---|---|
| `!commande.radioPlayer.isPlaying()` | Évite un double-démarrage si la radio joue déjà |
| `try/catch IllegalStateException` | Gère proprement le cas où `prepareAsync()` n'est pas encore terminé |

Si le `MediaPlayer` est encore en état `PREPARING` lors du retour dans l'application, le `catch` intercepte l'exception silencieusement. `onPrepared()` prendra le relais et appellera `start()` dès que le flux sera prêt — et vérifiera `isOnApp` à ce moment-là.

---

## 6. Flux corrigé complet

```
Utilisateur dit "Lance la radio RTL"
             │
             ▼
      CMD_RADIO() [main thread]
             │
             ▼
   Thread d'arrière-plan
   ├── appel API → token
   ├── appel API → nom de la radio
   └── appel API → URL du flux
             │
             ▼
   new Handler(Looper.getMainLooper()).post(() -> playRadio(url))
                                    [Fix 1 — retour sur main thread]
             │
             ▼
      playRadio() [main thread]
      ├── new MediaPlayer()
      ├── setPlayingRadio(true)
      ├── prepareAsync()       ← chargement asynchrone ~2-3s
      └── setOnPreparedListener(...)
             │
             │   [L'utilisateur ouvre BuddyCore pendant le chargement]
             │
             ▼
      onPause() → isOnApp = false
             │
             │   [prepareAsync() se termine]
             │
             ▼
      onPrepared() [main thread]
      └── isOnApp == false ?                [Fix 2 — vérification]
          → start() IGNORÉ ✅
             │
             │   [L'utilisateur ferme BuddyCore]
             │
             ▼
      onResume()
      ├── isPlayingRadio == true
      ├── radioPlayer != null
      ├── try { !isPlaying() → start() } ✅  [Fix 3 — protection]
      └── catch IllegalStateException → log  [Fix 3 — sécurité]
             │
             ▼
      Radio joue correctement ✅
             │
             │   [L'utilisateur rouvre BuddyCore]
             │
             ▼
      onPause()
      └── radioPlayer.pause() ✅
          isPlaying = true (mémorisé pour onResume)
```

---

## 7. Résumé

| # | Cause du bug | Correction appliquée | Fichier |
|---|---|---|---|
| 1 | `MediaPlayer` créé sur un thread sans Looper → `onPrepared` non fiable | `Handler(Looper.getMainLooper()).post(() -> playRadio(url))` | `Commande.java` ~4877 |
| 2 | `onPrepared` appelait `start()` sans vérifier si l'app était au premier plan | Ajout de `if (teamChatBuddyApplication.isOnApp)` dans `onPrepared` | `Commande.java` ~7133 |
| 3 | `onResume()` appelait `start()` sans vérifier l'état du `MediaPlayer` | Ajout de `isPlaying()` et `try/catch IllegalStateException` | `MainFragment.java` ~963 |

**Principe général retenu :** Toute interaction avec `MediaPlayer` doit se faire sur le thread principal, et toute décision de démarrage doit être conditionnée à la vérification de l'état de l'application (`isOnApp`) afin de respecter le cycle de vie Android.
