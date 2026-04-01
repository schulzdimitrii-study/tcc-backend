#!/bin/bash

RECIPIENTS="$NOTIFICATION_RECIPIENTS"
SUBJECT="CI Notification"
BODY="CI pipeline has just been executed successfully!"

echo "$BODY" | mail -s "$SUBJECT" "$RECIPIENTS"