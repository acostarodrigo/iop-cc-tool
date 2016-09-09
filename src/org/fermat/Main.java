package org.fermat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.cli.*;
import org.fermat.blockchain.MinerWhiteListTransaction;
import org.fermat.blockchain.TransactionSummary;
import org.fermatj.core.*;
import org.fermatj.params.MainNetParams;
import org.fermatj.params.RegTestParams;
import org.fermatj.params.TestNet3Params;

import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.Console;
import java.security.InvalidParameterException;


public class Main {
    // class variables
    private static Options options;
    private static MinerWhiteListTransaction.Action action;
    private static String masterPrivKey;
    private static String minerPublicKey;
    public static NetworkParameters networkParameters;
    public static Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);


    public static void main(String[] args)  {
        // create Options object
        options = addCommandLineOptions();

        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine commandLineline = parser.parse(options, args);

            //show only the version
            if (commandLineline.hasOption("v")){
                showVersion();
                System.exit(0);
            }

            // show only the help
            if (commandLineline.hasOption("h")){
                showHelp();
                System.exit(0);
            }

            // define debugging mode.
            if (commandLineline.hasOption("d"))
                logger.setLevel(Level.DEBUG);
            else
                //sets default logging level to ERROR
                logger.setLevel(Level.ERROR);



            //assign the rest of the mandatory arguments
            defineArguments(commandLineline);

        }
        catch( Exception e) {
            // oops, something went wrong
            System.err.println(e.getMessage());
            showHelp();
            System.exit(-1);
        }

        // make sure the private key is valid
        if (!isMasterPrivateKeyValid(masterPrivKey)){
            // oops, something went wrong
            System.err.println("Master private key is not valid on network " + networkParameters.getPaymentProtocolId() + ".");
            System.exit(-1);

        }

        // make sure the public key is valid
        if (!isMinerPublicKeyValid(minerPublicKey)){
            // oops, something went wrong
            System.err.println("Miner's public key is not valid on network " + networkParameters.getPaymentProtocolId() + ".");
            System.exit(-1);

        }

        try{
            /**
             * generates the transaction
             */
            MinerWhiteListTransaction generator = new MinerWhiteListTransaction(masterPrivKey, action, minerPublicKey);
            Transaction transaction = generator.build();

            // shows the summary and waits for confirmation
            showSummary(transaction);

            waitForEnter();
            generator.broadcast();
            System.exit(0);
        } catch (Exception exception){
            System.err.println(exception.getMessage());
            System.exit(-1);
        }
    }

    /**
     * validate the private key is valid.
     * @param masterPrivKey the hex string of the private key
     * @return true if valid.
     */
    private static boolean isMasterPrivateKeyValid(String masterPrivKey) {
        try {
            DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, masterPrivKey);
            ECKey privateKey = dumpedPrivateKey.getKey();
            if (privateKey.isPubKeyOnly())
                return false;
        } catch (AddressFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * waits for confirmation from the user
     * @return
     */
    private static boolean getConfirmation() {
        
        return false;
    }

    /**
     * shows on screen the summary of the output transaction
     * @param transaction
     */
    private static void showSummary(Transaction transaction) {
        TransactionSummary summary = new TransactionSummary(transaction);
        summary.showSummary();
    }

    /**
     * validates the public key is in a valid format
     * @param minerPublicKey
     * @return
     */
    private static boolean isMinerPublicKeyValid(String minerPublicKey) {
        ECKey publicKey = null;
        try{
            publicKey = ECKey.fromPublicOnly(Hex.decode(minerPublicKey));
            if (!publicKey.isPubKeyOnly())
                return false;
        }  catch (Exception e){
            return false;
        }
        return true;
    }

    private static void defineArguments(CommandLine commandLineline) {
        // make sure required parameters have data.
        if (!commandLineline.hasOption("p") || !commandLineline.hasOption("P") || !commandLineline.hasOption("a")){
            String output = "Required parameter missing.\n Private Key [p], Public Key [P] and Action [a] are required parameters.";
            throw new RuntimeException(output);
        }
        //master private key
        masterPrivKey = commandLineline.getOptionValue("P");
        // miner's public key
        minerPublicKey = commandLineline.getOptionValue("p");
        
        // action
        switch (commandLineline.getOptionValue("a").toLowerCase()){
            case "add":
                action = MinerWhiteListTransaction.Action.ADD;
                break;
            case "rem":
                action = MinerWhiteListTransaction.Action.REM;
                break;
            default:
                throw new InvalidParameterException(commandLineline.getOptionValue("a") + " is not a valid action parameter.");
        }

        // define the network, if any. RegTest by default.
        if (commandLineline.hasOption("n")) {
            switch (commandLineline.getOptionValue("n").toUpperCase()){
                case "MAIN":
                    networkParameters = MainNetParams.get();
                    break;
                case "TEST":
                    networkParameters = TestNet3Params.get();
                    break;
                case "REGTEST":
                    networkParameters = RegTestParams.get();
                    break;
                default:
                    throw new InvalidParameterException(commandLineline.getOptionValue("n") + " is not a valid parameter for Network.");
            }
        } else
            networkParameters = MainNetParams.get();

    }

    private static void showVersion(){
        System.out.println("IoP-Blockchain-Tool version 1.0");
    }

    /**
     * Setups all possible command arguments-
     * @return the Options with each configured option available
     */
    private static Options addCommandLineOptions (){
        Options options = new Options();

        // add Action option
        Option action = new Option("a", "action", true, "ADD or REM a public key to the blockchain");
        action.setOptionalArg(false);
        options.addOption(action);

        // add private key option
        Option privateKey = new Option("P", "Private", true, "Master private key");
        privateKey.setOptionalArg(false);
        options.addOption(privateKey);

        // add public key option
        Option publicKey = new Option("p", "public", true, "Miner's public key");
        publicKey.setOptionalArg(false);
        options.addOption(publicKey);

        // add network option
        Option network = new Option("n", "network", true, "MAIN, TEST, REGTEST networks. Default is MAIN");
        network.setOptionalArg(false);
        options.addOption(network);


        //add version option
        Option version = new Option("v", "version", false, "Print the version information and exit" );
        version.setRequired(false);
        options.addOption(version);

        //add debug option
        Option debug = new Option( "d", "debug", false, "Print debugging information. Disabled by default." );
        debug.setRequired(false);
        options.addOption(debug);

        // add help option
        Option help = new Option( "h", "help", false, "Print this message" );
        help.setRequired(false);
        options.addOption(help);

        return options;
    }

    /**
     * shows the commands help
     */
    private static void showHelp(){
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("IoP-Blockchain-Tool", options, true);
    }


    public static void waitForEnter() {
        Console c = System.console();
        if (c != null) {
            // printf-like arguments
            c.format("\nPress ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.\n");
            c.readLine();
        }
    }
}
