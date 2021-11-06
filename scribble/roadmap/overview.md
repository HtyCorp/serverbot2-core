## Roadmap Tasks (descending priority)

### Bring back Xray trace support

#### Status
Done

#### Complexity
Low

#### Impact
High ops impact, will improve understanding of command latency

#### Tasks
* Fix native-image incompatibility issues (mainly Apache Commons Logging usage)
* Add implementation in `XrayUtils`

### Upgrade to CDK Pipelines v2

#### Status
On hold (missing API features: no build caching)

#### Complexity
Low

#### Impact
Big improvements to ops for deployments

#### Tasks
* Migrate pipeline code to use new API
* May need to implement asset stripping to solve pipeline artifact size limit for CFN (unblocks new service additions)

### Improved UX for commands in Discord

#### Status
Planned

#### Complexity
Medium

#### Impact
High UX impact, improves problematic rough edges on commands

#### Tasks
* Migrate commands (currently in CommandService) to a standard model package
* Investigate ephemeral (per-game) threads for status messaging
* Rework slash command registration to use model directly from relay code
* Replace DMs for private links with ephemeral messages
* Implement assisted flows using Discord message components for UI

### Create instance workflow service

#### Status
Planned

#### Complexity
Medium

#### Impact
High ops impact in replacing existing hacky solution

#### Tasks
* Design new SFn flows with more decision branching and implement internally
* Expose state change APIs with strong consistency
* Use snapshots as game state source of truth (not EBS volumes as is the case now)
* Implement option for EC2 Spot usage
* Implement option for EBS volume preloading
* Add reboot/restart command

### Improved server admin tools

#### Status
Planned

#### Complexity
Medium

#### Tasks
* Service-stored (off-host) launch config
* LinuxGSM as install/start/stop/update provider
* Console read/write slash commands
* Spike (1): custom Xterm.js terminal for SSH (replaces default Session Manager)
* Spike (2): custom Xterm.js terminal for game server 

### Improved web look and feel for IPAuth pages

#### Status
Planned

#### Complexity
Low

#### Impact
Medium impact to UX as plain TXT on IP Auth is an eyesore. Unblocks auto-invoke onboarding later

#### Tasks
* Replace API-generated page with formatted static (scripted: JS/TS) CloudFront/S3 page
* Implement mini REST API for deferred post-load auth API call

### IOT Core for instance messaging

#### Status
Planned

#### Complexity
Medium

#### Impact
High ops improvement: makes two-way SQS obsolete and unblocks on-host firewall

#### Tasks
* Implement new service based on IOT Core for bi-directional request/response to/from instances
* Migrate app-daemon to IOT Core (probably rename it too)

### On-host firewall management

#### Status
Planned

#### Complexity
Medium

#### Impact
High UX: fixes long-standing issues with prefix list capacity and VPCFL delay

#### Tasks
* Add native libnftables integration to agent
* Replace core NetSec functionality with IOT messaging to on-host nftables
* Open non-privileged network ports by default (remove /openport, /closeport)

### IPAuth auto-invoke with web push

#### Status
Unplanned (nice to have, but expecting low adoption)

#### Complexity
Medium

#### Impact
Medium UX improvement as it makes IP auth smoother, potentially even auto in some cases (e.g. on /start)

#### Tasks
* Investigate best way to store user preference and onboard status
* Implement mini SFn workflow to account for web push latency possibly pushing command invoke latency >3s
* Update /addip implementation to use new workflow
* Add onboarding option to IPAuth page

### Multi-tenant bot

#### Status
Unplanned (stretch goal)

#### Complexity
Very high

#### Impact
Makes public launch possible, improves cost analysis

#### Tasks
* New service to securely fetch/cache customer credentials and audit access
* Figure out account onboarding flow (may be manual/CFN for now, but assisted would be nice in future)
* Rework DB schemas to make sense with multiple customers
* (Possibly) New service to store/manage customer details ('subscriptions'?)
* Rework service code to be aware of customer context
* New service based on IOT Core for bi-directional request/response to/from instances
* Migrate app-daemon to IOT Core (probably rename it too)
* Migrate to on-host firewall using nftables (replaces NetSec functionality)

### Use on-host OS SFTP server instead of embedded app-daemon version

#### Status
Unplanned (dependent on embedded SFTP server staying functional)

#### Complexity
Low

#### Impact
Medium ops impact, should mitigate excessive agent size

#### Tasks
* Investigate agent size reduction achieved by removing Apache MINA SSHD
* Implement mini custom PAM module for SFTP
* Update instance init to set up second OpenSSH sftp service
* Update agent SFTP logic to just manage second OS service
