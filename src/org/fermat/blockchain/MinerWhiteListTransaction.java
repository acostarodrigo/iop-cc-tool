package org.fermat.blockchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermatj.core.*;
import org.fermatj.script.Script;
import org.fermatj.script.ScriptBuilder;
import org.spongycastle.util.encoders.Hex;

/**
 * Created by rodrigo on 8/30/16.
 */
public class MinerWhiteListTransaction {
    private final String privateKey;
    private final String publicKey;
    private final Action action;

    private FermatNetwork fermatNetwork;
    private Transaction transaction;

    private final Logger logger = Main.logger;

    //Action Enum
    public enum Action{
        ADD , REM
    }

    // constructor
    public MinerWhiteListTransaction(String privateKey, Action action, String publicKey) {
        //preconditions check
        Preconditions.checkArgument(!privateKey.isEmpty());
        Preconditions.checkNotNull(action);
        Preconditions.checkArgument(!publicKey.isEmpty());

        if (logger.getLevel() != Level.DEBUG)
            logger.setLevel(Level.OFF);

        this.privateKey = privateKey;
        this.action = action;
        this.publicKey = publicKey;
    }

    /**
     * construct the OP_Return data to include in the transaction
     * @return
     */
    private String getOP_Return (){
        String data;
        switch (this.action){
            case ADD:
                data =  "add".concat(this.publicKey);
                break;
            case REM:
                data =  "rem".concat(this.publicKey);
                break;
            default:
                data =  "add".concat(this.publicKey);
                break;
        }

        /**
         * we are limiting the output to lenght 40
         */
        if (data.length() > 40){
            data = data.substring(0, 40);
        }

        return data;
    }

    /**
     * Builds the transaction to be sent.
     * @return
     * @throws CantConnectToFermatBlockchainException
     * @throws InsufficientMoneyException
     * @throws TransactionErrorException
     * @throws AddressFormatException
     */
    public Transaction build() throws CantConnectToFermatBlockchainException, InsufficientMoneyException, TransactionErrorException, AddressFormatException {
        // start the network with the passed private key
        fermatNetwork = new FermatNetwork(this.privateKey);
        fermatNetwork.initialize();

        // import the private key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(FermatNetwork.NETWORK, this.privateKey);
        ECKey key = dumpedPrivateKey.getKey();

        /**
         * We must have positive confirmed balance in order to generate the transaction.
         */
        Wallet wallet = fermatNetwork.getFermatWallet();
        if (wallet.getBalance(Wallet.BalanceType.AVAILABLE).isZero())
            throw new CantConnectToFermatBlockchainException("Wallet balance is zero");

        Wallet.SendRequest sendRequest = Wallet.SendRequest.to(FermatNetwork.NETWORK, key, Coin.CENT);

        // we are returning any change to the same address
        sendRequest.changeAddress = key.toAddress(FermatNetwork.NETWORK);

        this.transaction = sendRequest.tx;

        // add the public key into the op_Return output.
        Script op_return = ScriptBuilder.createOpReturnScript(getOP_Return().getBytes());
        TransactionOutput output = new TransactionOutput(FermatNetwork.NETWORK, transaction, Coin.ZERO, op_return.getProgram());
        transaction.addOutput(output);

        // we complete the transaction
        wallet.completeTx(sendRequest);
        return transaction;
    }

    /**
     * broadcast the generated transaction
     * @throws TransactionErrorException
     */
    public void broadcast() throws TransactionErrorException {
        Preconditions.checkNotNull(this.transaction);
        fermatNetwork.broadcast(this.transaction);
    }

    /**
     * broadcast the passed transaction
     * @param transaction
     * @throws TransactionErrorException
     */
    public void broadcast(Transaction transaction) throws TransactionErrorException {
        Preconditions.checkNotNull(transaction);
        fermatNetwork.broadcast(transaction);
    }
}
