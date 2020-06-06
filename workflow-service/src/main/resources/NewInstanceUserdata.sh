#!/bin/sh

set -e

echo '[SB2INIT] Setting noninteractive mode for apt...'
export DEBIAN_FRONTEND=noninteractive

echo '[SB2INIT] Setting up Corretto and multiverse apt repositories...'
wget -O- https://apt.corretto.aws/corretto.key | sudo apt-key add -
add-apt-repository 'deb https://apt.corretto.aws stable main'
add-apt-repository multiverse
dpkg --add-architecture i386

echo '[SB2INIT] Running apt update...'
apt -y update

echo '[SB2INIT] Running apt upgrade...'
apt -y upgrade

echo '[SB2INIT] Installing corretto11...'
apt -y install java-11-amazon-corretto-jdk

echo '[SB2INIT] Installing apt-provided AWS CLI version for bootstrap...'
apt -y install awscli

echo '[SB2INIT] Installing steamcmd...'
apt -y install lib32gcc1 steamcmd

echo '[SB2INIT] Setting up working directory...'
mkdir /opt/serverbot2
mkdir /opt/serverbot2/daemon
mkdir /opt/serverbot2/config
mkdir /opt/serverbot2/game
mkdir /opt/serverbot2/logs
cd /opt/serverbot2

echo '[SB2INIT] Creating launch config example...'
cat > config/game.cfg << "EOF"
{
    "launchCmd": ["example.exe", "-game", "blah", "-port", "12345"]
}
EOF

echo '[SB2INIT] Setting up app daemon fetch script...'
cat > daemon/run_latest_daemon.sh << "EOF"
BUCKET=$(aws ssm get-parameter --name '/common-config/deployed-artifacts-bucket' --query Parameter.Value --output text)
aws s3 cp s3://$BUCKET/app-daemon.jar /opt/serverbot2/daemon/app-daemon.jar
java -jar /opt/serverbot2/daemon/app-daemon.jar
EOF
chmod 774 daemon/run_latest_daemon.sh

echo '[SB2INIT] Setting working directory owner to default EC2 user...'
chown -R ubuntu:ubuntu /opt/serverbot2

echo '[SB2INIT] Setting up app daemon service unit...'
cat >/etc/systemd/system/serverbot2.service << "EOF"
[Unit]
Description=Run serverbot2 app daemon
Requires=Network.target
After=Network.target
[Service]
Type=simple
User=ubuntu
ExecStart=/opt/serverbot2/daemon/run_daemon.sh
[Install]
WantedBy=multi-user.target
EOF

echo '[SB2INIT] Starting app daemon service...'
systemctl start serverbot2.service

echo '[SB2INIT] Init finished!'
