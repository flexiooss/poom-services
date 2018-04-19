#!/usr/bin/env sh

echo "starting service..."
export SERVICE_HOST=$(hostname -i)
exec java -cp "/var/service/lib/*:/var/service/config/" "$@"
echo "service stopped"