package org.fermat.blockchain;

import com.google.common.base.Preconditions;
import org.blockchainj.core.Transaction;
import org.blockchainj.core.TransactionOutput;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;

/**
 * Created by rodrigo on 9/8/16.
 */
public class TransactionSummary {
    private final Transaction transaction;

    //constructor
    public TransactionSummary(Transaction transaction) {
        Preconditions.checkNotNull(transaction);
        this.transaction = transaction;
    }

    /**
     * Gets the data of the OP_Return
     * @return null if not valid
     */
    private String getOutputData() throws UnsupportedEncodingException {
        String data = null;
        for (TransactionOutput output : this.transaction.getOutputs()){
            if (output.getScriptPubKey().isOpReturn()){
                data = new String(output.getScriptPubKey().getChunks().get(1).data, "UTF-8");
            }
        }

        if (data.length() != 40)
            throw new RuntimeException("Output data is not correct");

        return data;
    }

    public void showSummary(){
        String data = null;
        try {
            data = getOutputData();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("There was an error readying the Output data of the transaction.\nData: " + data);
        }

        try{
            StringBuilder output = new StringBuilder();

            MinerWhiteListTransaction.Action action = MinerWhiteListTransaction.Action.valueOf(data.substring(0,3).toUpperCase());

            output.append("Action: ");
            output.append(action.toString());
            output.append(System.lineSeparator());
            output.append("Public key: ");
            output.append(data.substring(3));
            System.out.println(output.toString());
        } catch (Exception e) {
            // if data is invalid I might have substring cut errors
            throw new RuntimeException("Invalid data extracted from transaction. " + transaction.toString() + "\nCan't go on");
        }
    }
}
