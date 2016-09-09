# IoP-Blockchain-Tool

Adds and removes  public keys into the blockchain for the miner white list control.

### Usage

Execute .jar app with -h for help on the parameters
```
$ java -jar iop-blockchain-tool/out/artifacts/iop_blockchain_tool_jar/iop-blockchain-tool.jar -h
```


```
usage: IoP-Blockchain-Tool [-a <arg>] [-d] [-h] [-n <arg>] [-P <arg>] [-p
       <arg>] [-v]
 -a,--action <arg>    ADD or REM a public key to the blockchain
 -d,--debug           Print debugging information. Disabled by default.
 -h,--help            Print this message
 -n,--network <arg>   MAIN, TEST, REGTEST networks. Default is MAIN
 -P,--Private <arg>   Master private key
 -p,--public <arg>    Miner's public key
 -v,--version         Print the version information and exit

```

Mandatory arguments are:

* -Action
 *ADD*:  adds a public key into the miner white list.
  *REM*:  removes a public key from the miner white list.
  
  * -Private: the **master private key** allowed to execute this transactions.
  
  * -public: the miner public key to add into the white list.
  
Default network is Mainnet. To switch, use the -network parameter. Example:

```
iop-blockchain-tool.jar -a add -P validPrivateKey -p validPublicKey -n Test
```

## Program description

The goal is to generate and broadcast a valid transaction on the IoP blockchain including the passed **Action** and **public key** into an *OP_RETURN* output for the IoP core client to process it.

The supplied **private key** is used to sign the transaction. The IoP core client detects transactions from this particular private key and based on the action specified, it will add or remove the public key from the client's white list database.

When the Miner white list control is activated in the *IoP blockchain* only blocks which include coinbase transactions signed from any of the public keys on the miner white list will be allowed to incorporate into the blockchain.

Example of an execution output:

```
iop-blockchain-tool.jar -a add -P ValidPublicKey -p 029443aea9b102504bc093a0b2a0b8afbf0eda4a55baf1123ca4e948a0669dff62 -n regtest
```

```
Connecting to IoP regtest network...
Connected to peer [127.0.0.1]:14877
Action: ADD
Public key: 029443aea9b102504bc093a0b2a0b8afbf0ed

Press ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.

Broadcasting transaction...
Transaction broadcasted sucessfully


```

## Authors

* **Rodrigo Acosta** - *Initial work* - [acostarodrigo](https://github.com/acostarodrigo)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details