# IoP-CC-Tool

Creates a new Contribution Contract and allows voting YES or NO for any contract.

### Usage

Execute .jar app with -h for help on the parameters
```
$ java -jar iop-blockchain-tool/out/artifacts/iop_blockchain_tool_jar/iop-blockchain-tool.jar -h
```


```
usage: IoP-CC-Tool [-a <arg>] [-d] [-h] [-n <arg>] [-P <arg>] [-p
       <arg>] [-v]
 -a,--action <arg>    NEW, VOTYES or VOTNO 
 -d,--debug           Print debugging information. Disabled by default.
 -h,--help            Print this message
 -n,--network <arg>   MAIN, TEST, REGTEST networks. Default is MAIN
 -P,--Private <arg>   Master private key
 -v,--values <arg>    if Action is NEW, specify the list of beneficiaries addresses. If Action is VOT, specify the CC tx Hash.
 -v,--version         Print the version information and exit

```

Mandatory arguments are:

* -Action
 *NEW*:  Creates a new Contribution Contract with a Block start of 10 and block end of 10. And a fixed reward of 80000 satoshis.
 *VOTYES*:  generates a positive Voting transaction for the specified CC tx hash
 *VOTYES*:  generates a negative Voting transaction for the specified CC tx hash
  
  * -Private: the **master private key** allowed to execute this transactions.
  
  
Default network is Mainnet. To switch, use the -network parameter. Example:

```
iop-cc-tool.jar -a add -P validPrivateKey -a NEW -v pTtMh5xvPn8Dxyy5tqFze1MPASvNLxU52E 
```

## Program description

Generates and broadcast Contribution Contract related transactions.


Example of an execution output:

```
iop-cc-tool.jar -a add -P validPrivateKey -a NEW -v pTtMh5xvPn8Dxyy5tqFze1MPASvNLxU52E 
```

```
Connecting to IoP regtest network...
Connected to peer [127.0.0.1]:14877
Action: BEW

Press ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.

Broadcasting transaction...
Transaction broadcasted sucessfully


```

## Authors

* **Rodrigo Acosta** - *Initial work* - [acostarodrigo](https://github.com/acostarodrigo)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
