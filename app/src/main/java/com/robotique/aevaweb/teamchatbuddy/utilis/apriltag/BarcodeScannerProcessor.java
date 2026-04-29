package com.robotique.aevaweb.teamchatbuddy.utilis.apriltag;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.robotique.aevaweb.teamchatbuddy.utilis.DetectionCallback;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Barcode Detector Demo. */
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>> {

    private static final String TAG = "BarcodeProcessor";

    private final BarcodeScanner barcodeScanner;
    private final DetectionCallback detectionCallback;

    public BarcodeScannerProcessor(Context context, DetectionCallback detectionCallback, BarcodeScannerOptions options) {
        super(context);

        this.detectionCallback = detectionCallback;
        barcodeScanner = BarcodeScanning.getClient(options);

    }

    @Override
    public void stop() {
        super.stop();
        barcodeScanner.close();
    }

    @Override
    protected Task<List<Barcode>> detectInImage(Bitmap image, int rotation) {
        return barcodeScanner.process(image, rotation);
    }

    @Override
    protected void onSuccess(
            @NonNull List<Barcode> barcodes) {
        if (barcodes.isEmpty()) {
            detectionCallback.onNoDetection();
            //Log.v(MANUAL_TESTING_LOG, "No barcode has been detected");
        }
        for (int i = 0; i < barcodes.size(); ++i) {
            Barcode barcode = barcodes.get(i);
            String rawValue = barcode.getRawValue();
            if (rawValue == null && barcode.getRawBytes() != null) {
                rawValue = new String(barcode.getRawBytes(), StandardCharsets.ISO_8859_1);
            }
            detectionCallback.onDetection(rawValue + ";" + getFormatName(barcode.getFormat()));

            Log.i("MYA_QR", "run: qr/matrix " + rawValue + ";" + getFormatName(barcode.getFormat()));
            logExtrasForTesting(barcode);
        }
    }

    private String getFormatName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE:
                return "QRCode";
            case Barcode.FORMAT_DATA_MATRIX:
                return "DataMatrix";
        }
        return null;
    }


    private static void logExtrasForTesting(Barcode barcode) {
        if (barcode != null) {
            if (barcode.getBoundingBox() != null) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        String.format(
                                "Detected barcode's bounding box: %s", barcode.getBoundingBox().flattenToString()));
            }
            if (barcode.getCornerPoints() != null) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        String.format(
                                "Expected corner point size is 4, get %d", barcode.getCornerPoints().length));
            }
            for (Point point : barcode.getCornerPoints()) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Corner point is located at: x = %d, y = %d", point.x, point.y));
            }
            Log.v(MANUAL_TESTING_LOG, "barcode display value: " + barcode.getDisplayValue());
            Log.v(MANUAL_TESTING_LOG, "barcode raw value: " + barcode.getRawValue());
            Barcode.DriverLicense dl = barcode.getDriverLicense();
            if (dl != null) {
                Log.v(MANUAL_TESTING_LOG, "driver license city: " + dl.getAddressCity());
                Log.v(MANUAL_TESTING_LOG, "driver license state: " + dl.getAddressState());
                Log.v(MANUAL_TESTING_LOG, "driver license street: " + dl.getAddressStreet());
                Log.v(MANUAL_TESTING_LOG, "driver license zip code: " + dl.getAddressZip());
                Log.v(MANUAL_TESTING_LOG, "driver license birthday: " + dl.getBirthDate());
                Log.v(MANUAL_TESTING_LOG, "driver license document type: " + dl.getDocumentType());
                Log.v(MANUAL_TESTING_LOG, "driver license expiry date: " + dl.getExpiryDate());
                Log.v(MANUAL_TESTING_LOG, "driver license first name: " + dl.getFirstName());
                Log.v(MANUAL_TESTING_LOG, "driver license middle name: " + dl.getMiddleName());
                Log.v(MANUAL_TESTING_LOG, "driver license last name: " + dl.getLastName());
                Log.v(MANUAL_TESTING_LOG, "driver license gender: " + dl.getGender());
                Log.v(MANUAL_TESTING_LOG, "driver license issue date: " + dl.getIssueDate());
                Log.v(MANUAL_TESTING_LOG, "driver license issue country: " + dl.getIssuingCountry());
                Log.v(MANUAL_TESTING_LOG, "driver license number: " + dl.getLicenseNumber());
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }

}