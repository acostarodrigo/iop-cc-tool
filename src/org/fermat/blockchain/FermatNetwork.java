package org.fermat.blockchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.blockchainj.params.IoP.IoP_RegTestParams;
import org.blockchainj.wallet.Wallet;
import org.fermat.Main;
import org.blockchainj.core.*;
import org.blockchainj.net.discovery.DnsDiscovery;
import org.blockchainj.params.RegTestParams;
import org.blockchainj.store.BlockStore;
import org.blockchainj.store.BlockStoreException;
import org.blockchainj.store.MemoryBlockStore;
import org.blockchainj.wallet.WalletTransaction;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by rodrigo on 7/22/16.
 */
public class FermatNetwork {
    /**
     * class variables
     */
    private Wallet wallet;
    private BlockStore blockStore;
    private PeerGroup peerGroup;
    private BlockChain blockChain;
    private BlockchainEvents events;
    private Transaction genesisTransaction;
    private int minBroadcastConnections;

    /**
     * class constants
     */
    public static final NetworkParameters NETWORK = Main.networkParameters;
    private final Context CONTEXT = Context.getOrCreate(NETWORK);
    private final String MINING_PRIVATE_KEY;

    private final Logger logger = Main.logger;

    /**
     * constructor
     * @param privateKey
     */
    public FermatNetwork(String privateKey) {
        Preconditions.checkNotNull(privateKey);

        // I will hide IoPj output
        if (logger.getLevel() != Level.DEBUG)
            logger.setLevel(Level.OFF);

        logger.setAdditive(false);

        this.MINING_PRIVATE_KEY = privateKey;
    }


    public void initialize() throws CantConnectToFermatBlockchainException {
        // get the wallet
        this.wallet = getWallet();

        if (!isPrivateKeyInWallet())
            throw new CantConnectToFermatBlockchainException("No imported key in wallet to claim for positive balance. Can't go on");

        events = new BlockchainEvents();


        try {
            this.blockChain = getBlockchain();
        } catch (BlockStoreException e) {
            throw new CantConnectToFermatBlockchainException("Unable to create a blockchain.", e);
        }


        peerGroup = new PeerGroup(CONTEXT, blockChain);
        peerGroup.addWallet(wallet);


        // if this is reg test, we are connecting to local and only wait one confirmation
        if (NETWORK == IoP_RegTestParams.get())
        {
            peerGroup.setUseLocalhostPeerWhenPossible(true);
            peerGroup.addAddress(new PeerAddress(new InetSocketAddress("127.0.0.1", 7685)));
            minBroadcastConnections = 1;
        } else {
            peerGroup.addPeerDiscovery(new DnsDiscovery(NETWORK));
            minBroadcastConnections = 2;
        }

        System.out.println("Connecting to IoP " + NETWORK.getPaymentProtocolId() + " network...");
        try{
            peerGroup.start();
            peerGroup.downloadBlockChain();
        } catch (Exception e){
            throw new CantConnectToFermatBlockchainException("There was a problem connecting and downloading the blockchain on the specified network.", e);
        }

        // lets make sure the private key imported give us IoPs!
        if (wallet.getBalance(Wallet.BalanceType.AVAILABLE).isZero())
            throw new CantConnectToFermatBlockchainException("After blockchain download completed, no UTXO transactions where found. Possible wrong private key.\nCan't go on without coins.");

        genesisTransaction = wallet.getTransactionPool(WalletTransaction.Pool.UNSPENT).values().iterator().next();
        if (genesisTransaction == null)
            throw new CantConnectToFermatBlockchainException("After blockchain download completed, no UTXO transactions where found. Possible wrong private key.\nCan't go on without coins.");
    }

    private BlockChain getBlockchain() throws BlockStoreException {
        Preconditions.checkNotNull(wallet);

        blockStore = new MemoryBlockStore(CONTEXT.getParams());

        return new BlockChain(CONTEXT, wallet, blockStore);
    }

    /**
     * get the wallet
     * @return
     * @throws CantConnectToFermatBlockchainException
     */
    private Wallet getWallet() throws CantConnectToFermatBlockchainException {
        Wallet internalWallet = null;
            try {
                internalWallet = new Wallet(CONTEXT);

                //import the private key into the wallet.
                internalWallet.importKey(getPrivateKeyFromDumpKey());


            } catch (AddressFormatException e) {
                //if I can't get the ECKey to import I can't go on.
                throw new CantConnectToFermatBlockchainException("The private key provided " + this.MINING_PRIVATE_KEY + " is not valid.");
            }
        return internalWallet;
    }

    /**
     * true if the key is in the wallet.
     * @return
     */
    private boolean isPrivateKeyInWallet(){
        Preconditions.checkNotNull(wallet);
        ECKey ecKey = null;
        try {
            ecKey = getPrivateKeyFromDumpKey();
        } catch (AddressFormatException e) {
            // I will validate to false
            return false;
        }

        for (ECKey importedKeys : wallet.getImportedKeys()){
            if (importedKeys.equals(ecKey))
                return true;
        }
        return false;
    }

    /**
     * Transform the dumpedPrivateKey provided at startup into an ECKey
     * @return
     * @throws AddressFormatException
     */
    private ECKey getPrivateKeyFromDumpKey() throws AddressFormatException {
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(CONTEXT.getParams(), this.MINING_PRIVATE_KEY);
        return dumpedPrivateKey.getKey();
    }

    /**
     * gets the wallet
     * @return
     */
    public Wallet getFermatWallet(){
        return this.wallet;
    }

    /**
     * the genesis transaction
     * @return
     */
    public Transaction getGenesisTransaction() {
        return genesisTransaction;
    }

    /**
     * broadcast the passed transaction
     * @param transaction
     */
    public void broadcast(Transaction transaction) throws TransactionErrorException {
        System.out.println("Broadcasting transaction...");
        try {
            TransactionBroadcast transactionBroadcast = peerGroup.broadcastTransaction(transaction);
            transactionBroadcast.setMinConnections(minBroadcastConnections);

             ListenableFuture<Transaction> future = transactionBroadcast.broadcast();

            Futures.addCallback(future, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction transaction) {
                    System.out.println("Transaction broadcasted sucessfully");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            );

            future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new TransactionErrorException("There was a problem broadcasting the passed transaction. " + transaction.toString(), e);
        }




    }

}
