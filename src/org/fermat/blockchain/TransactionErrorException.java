package org.fermat.blockchain;

/**
 * Created by rodrigo on 8/15/16.
 */
public class TransactionErrorException extends Exception {
    public TransactionErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
