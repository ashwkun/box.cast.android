import os
import json
import firebase_admin
from firebase_admin import credentials, db

def main():
    creds_json = os.environ.get('FIREBASE_CREDENTIALS')
    cred = credentials.Certificate(json.loads(creds_json))
    firebase_admin.initialize_app(cred, {"databaseURL": "https://boxcasts-default-rtdb.asia-southeast1.firebasedatabase.app"})
    
    # Store the proxy key securely in RTDB
    db.reference("secrets/app_secret").set("boxcast-app-c47580b27dd94ec1")
    print("Stored secrets/app_secret in RTDB")

if __name__ == "__main__":
    main()
