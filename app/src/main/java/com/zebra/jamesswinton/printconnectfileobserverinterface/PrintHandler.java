package com.zebra.jamesswinton.printconnectfileobserverinterface;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Parcel;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import java.util.HashMap;

public class PrintHandler {

    // Debugging
    private static final String TAG = "PrintHandler";

    // Constants


    // Private Variables
    private static final String PRINT_CONNECT_PACKAGE = "com.zebra.printconnect";
    private static final String TEMPLATE_DATA = "com.zebra.printconnect.PrintService.TEMPLATE_DATA";
    private static final String VARIABLE_DATA = "com.zebra.printconnect.PrintService.VARIABLE_DATA";
    private static final String RESULT_RECEIVER = "com.zebra.printconnect.PrintService.RESULT_RECEIVER";
    private static final String PRINT_WITH_CONTENT_SERVICE = "com.zebra.printconnect.print.TemplatePrintWithContentService";

    // Public Variables


    /**
     * Public Utility Methods
     */

    // Sends Print job via intent to PrintConnect service - Uses template & variable data
    public static void sendPrintJobWithContent(Context context, byte[] templateBytes,
                                               @Nullable HashMap<String, String> variableData,
                                               ResultReceiver resultReceiver) {
        Intent sendPrintJob = new Intent();
        sendPrintJob.setComponent(new ComponentName(PRINT_CONNECT_PACKAGE, PRINT_WITH_CONTENT_SERVICE));
        sendPrintJob.putExtra(TEMPLATE_DATA, templateBytes); // Template ZPL as UTF-8 encoded byte array
        sendPrintJob.putExtra(VARIABLE_DATA, variableData);
        sendPrintJob.putExtra(RESULT_RECEIVER, buildSafeReceiver(resultReceiver));
        context.startService(sendPrintJob);
    }

    /**
     * Private Utility Methods
     */

    // This method makes your ResultReceiver safe for inter-process communication
    private static ResultReceiver buildSafeReceiver(ResultReceiver actualReceiver) {
        Parcel parcel = Parcel.obtain();
        actualReceiver.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }
}
