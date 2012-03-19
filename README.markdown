The _AndroidMarketArchiver_ helps you keep a record of your App's statistics 
data as presented in Android Market developer console.

It logs into the Market Developer Website on your behalf and grabs a snapshot of the relevant data.

For getting a data snapshot call 

		./grab.sh <your Google Account> <your Google Password>
(The script is just a wrapper around a java call, so should be easily portable to shell-free platforms.)

The script is set up for a market console with one app, use `-apps <number>` in `grab.sh` if you have multiple apps in the market.