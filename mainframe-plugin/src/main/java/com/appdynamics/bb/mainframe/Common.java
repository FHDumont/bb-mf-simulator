package com.appdynamics.bb.mainframe;

import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.Transaction;

public class Common {

    public static boolean isFakeTransaction(Transaction transaction) {
        return transaction == null || "".equals(transaction.getUniqueIdentifier());
    }

    public static boolean isFakeExitCall(ExitCall exitCall) {
        return exitCall == null || "".equals(exitCall.getCorrelationHeader());
    }
}
