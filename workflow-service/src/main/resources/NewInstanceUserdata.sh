#!/bin/sh

set -e

echo '[SB2INIT] Setting noninteractive mode for apt...'
export DEBIAN_FRONTEND=noninteractive

echo '[SB2INIT] Setting up Corretto and multiverse apt repositories...'
wget -O- https://apt.corretto.aws/corretto.key | sudo apt-key add -
add-apt-repository 'deb https://apt.corretto.aws stable main'
add-apt-repository multiverse
dpkg --add-architecture i386

echo '[SB2INIT] Running  update...'
apt-get -y update

echo '[SB2INIT] Running  upgrade...'
apt-get -y upgrade

echo '[SB2INIT] Installing corretto11...'
apt-get -y install java-11-amazon-corretto-jdk

echo '[SB2INIT] Installing and configuring apt-provided AWS CLI version...'
apt-get -y install awscli jq
AWS_REGION=$(curl -s 'http://169.254.169.254/latest/dynamic/instance-identity/document' | jq -r .region)
sudo -Hu ubuntu aws configure set region $AWS_REGION

echo '[SB2INIT] Installing steamcmd...'
# steamcmd requires an EULA agreement - must preset for noninteractive
echo steam steam/license note '' | debconf-set-selections
echo steam steam/question select 'I AGREE' | debconf-set-selections
apt-get -y install lib32gcc1 steamcmd

echo '[SB2INIT] Setting up working directory...'
mkdir /opt/serverbot2
mkdir /opt/serverbot2/daemon
mkdir /opt/serverbot2/config
mkdir /opt/serverbot2/game
mkdir /opt/serverbot2/logs
cd /opt/serverbot2

echo '[SB2INIT] Creating launch config example...'
cat > config/launch.cfg << "EOF"
{
    "launchCommand": ["python3", "-m", "http.server", "8080"],
    "relativePath": "false",
    "environment": {
        "PYTHONUNBUFFERED": "true"
    }
}
EOF

echo '[SB2INIT] Setting up app daemon fetch script...'
cat > daemon/run_daemon.sh << "EOF"
#!/bin/sh
echo "[INFO] Locating app daemon artifact..."
PARAM_NAME='/app-instance-share/public/app-daemon-jar-s3-url'
JAR_S3_URL=$(aws ssm get-parameter --name "$PARAM_NAME"  --query Parameter.Value --output text)
echo "[INFO] Artifact S3 URL is '$JAR_S3_URL'"
aws s3 cp "$JAR_S3_URL" /opt/serverbot2/daemon/app-daemon.jar
echo "[INFO] Running daemon executable..."
java -jar /opt/serverbot2/daemon/app-daemon.jar
EOF
chmod 774 daemon/run_daemon.sh

echo '[SB2INIT] Setting working directory owner to default EC2 user...'
chown -R ubuntu:ubuntu /opt/serverbot2

echo '[SB2INIT] Setting up app daemon service unit...'
cat >/etc/systemd/system/serverbot2.service << "EOF"
[Unit]
Description=Run serverbot2 app daemon
Requires=network.target
After=network.target
[Service]
Type=simple
User=ubuntu
ExecStart=/opt/serverbot2/daemon/run_daemon.sh
[Install]
WantedBy=multi-user.target
EOF

echo '[SB2INIT] Enabling app daemon service for future launches...'
systemctl enable serverbot2

echo '[SB2INIT] Starting app daemon service...'
systemctl start serverbot2

echo '[SB2INIT] Init finished!'
