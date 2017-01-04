package org.fermat.blockchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.blockchainj.params.IoP.IoP_RegTestParams;
import org.blockchainj.wallet.SendRequest;
import org.blockchainj.wallet.Wallet;
import org.fermat.Main;
import org.blockchainj.core.*;
import org.blockchainj.script.Script;
import org.blockchainj.script.ScriptBuilder;
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

    private final String values;

    //Action Enum
    public enum Action{
        NEW , VOTYES, VOTNO
    }

    // constructor
    public MinerWhiteListTransaction(String privateKey, Action action, String values, String publicKey) {
        //preconditions check
        Preconditions.checkArgument(!privateKey.isEmpty());
        Preconditions.checkNotNull(action);
        Preconditions.checkArgument(!values.isEmpty());

        if (logger.getLevel() != Level.DEBUG)
            logger.setLevel(Level.OFF);

        this.privateKey = privateKey;
        this.action = action;
        this.values = values;
        this.publicKey = publicKey;
    }

    /**
     * construct the OP_Return data to include in the transaction
     * @return
     */
    private String getOP_Return (){
        String data;
        switch (this.action){
            case NEW:
                data =  "4343000100000a000a7a1200d4817aa5497628e7c77e6b606107042bbba3130888c5f47a375e6179be789fbb0017";
                break;
            case VOTYES:
                data =  "564f5401".concat(values);
                break;
            case VOTNO:
                data =  "564f5400".concat(values);
                break;
            default:
                data =  "";
                break;
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

        // if we are creating a new CC we need to have at least 1001 IoPs
        if (action == Action.NEW){
            if (wallet.getBalance(Wallet.BalanceType.AVAILABLE).isLessThan(Coin.COIN.multiply(1001)))
                throw new CantConnectToFermatBlockchainException("Wallet balance is less than 1000 IoPs required to generate a CC.");

        }

        Transaction genesisTransaction = wallet.getTransactions(false).iterator().next();
        TransactionOutput genesisOutput = null;

        Coin minValue = Coin.ZERO;

        if (action == Action.NEW)
            minValue = Coin.COIN.multiply(1000);
        else
            minValue = Coin.COIN.multiply(1);

        for (TransactionOutput output : genesisTransaction.getOutputs()){
            if (output.getValue().isGreaterThan(minValue))
                genesisOutput = output;
        }

        transaction = new Transaction(FermatNetwork.NETWORK);
        transaction.addInput(genesisOutput);


        //Freeze Address and also change address
        Address freezeAddres = key.toAddress(FermatNetwork.NETWORK);

        //freeze output
        Coin mandatoryValue = Coin.COIN.multiply(1000);
        transaction.addOutput(mandatoryValue, freezeAddres);

        //change output
        transaction.addOutput(transaction.getInput(0).getValue().subtract(minValue).subtract(Coin.valueOf(5000000)).subtract(Transaction.DEFAULT_TX_FEE), freezeAddres);

        // op_return output
        Script op_return = ScriptBuilder.createOpReturnScript(Hex.decode(getOP_Return()));
        TransactionOutput output = new TransactionOutput(FermatNetwork.NETWORK, transaction, Coin.ZERO, op_return.getProgram());
        transaction.addOutput(output);

        if (action == Action.NEW){
            // beneficiary outputs
            String[] beneficiaries = values.split(",");
            Coin beneficiaryReward = Coin.valueOf(80000).div(beneficiaries.length); // I calculate the total beneficiary distribution by dividing the hardcoded reward among all beneficiaries
            for (String add : beneficiaries){
                transaction.addOutput(beneficiaryReward, Address.fromBase58(IoP_RegTestParams.get(), add));
            }
        }


        SendRequest sendRequest = SendRequest.forTx(transaction);
        sendRequest.shuffleOutputs = false;
        wallet.signTransaction(sendRequest);

        // we complete the transaction
//        wallet.completeTx(sendRequest);
        System.out.println(transaction.toString());
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
