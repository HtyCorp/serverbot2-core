## About

Code for all software agents that run on the client/application side.

### `app-daemon`

_Runs on: deployed application EC2 instances_

An agent that runs on game/application EC2 instances to hook into with running workflows, run game/application processes and handle logging and idle shutdowns.

This is mainly interacted with via `services/workflows-handler`, using two-way SQS communication since it's cheap and relatively easy to implement. This isn't very secure (at least not secure enough to use publicly in a multi-tenant setup), so will be migrated to a more secure WebSocket design at a later time.

List of changes planned:
* Switch to WebSocket design with improved security/authentication.
* Implement pty support to make application logging work with more software.
* Deploy as a portable Java custom runtime image to remove dependency on system Java11 runtime (see `scribble/jigsaw.txt` for details).
