from flask import Flask, redirect, request, session, jsonify
from requests_oauthlib import OAuth1Session
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)

# Replace these values with your own consumer key and secret
CONSUMER_KEY = 'your_consumer_key'
CONSUMER_SECRET = 'your_consumer_secret'
REQUEST_TOKEN_URL = 'https://connectapi.garmin.com/oauth-service/oauth/request_token'
AUTHORIZE_URL = 'https://connect.garmin.com/oauthConfirm'
ACCESS_TOKEN_URL = 'https://connectapi.garmin.com/oauth-service/oauth/access_token'
CALLBACK_URI = 'http://127.0.0.1:5000/callback'  # Update this to your actual callback URL

@app.route('/start_oauth')
def start_oauth():
    oauth = OAuth1Session(CONSUMER_KEY, client_secret=CONSUMER_SECRET, callback_uri=CALLBACK_URI)
    try:
        fetch_response = oauth.fetch_request_token(REQUEST_TOKEN_URL)
    except ValueError:
        return jsonify({'error': 'Error fetching request token'}), 400

    session['resource_owner_key'] = fetch_response.get('oauth_token')
    session['resource_owner_secret'] = fetch_response.get('oauth_token_secret')
    authorization_url = oauth.authorization_url(AUTHORIZE_URL)

    return redirect(authorization_url)

@app.route('/callback')
def callback():
    oauth = OAuth1Session(CONSUMER_KEY,
                          client_secret=CONSUMER_SECRET,
                          resource_owner_key=session['resource_owner_key'],
                          resource_owner_secret=session['resource_owner_secret'],
                          verifier=request.args.get('oauth_verifier'))
    try:
        oauth_tokens = oauth.fetch_access_token(ACCESS_TOKEN_URL)
    except ValueError:
        return jsonify({'error': 'Error fetching access token'}), 400

    session['oauth_token'] = oauth_tokens.get('oauth_token')
    session['oauth_token_secret'] = oauth_tokens.get('oauth_token_secret')

    # Return the tokens to the app
    return jsonify({
        'oauth_token': oauth_tokens.get('oauth_token'),
        'oauth_token_secret': oauth_tokens.get('oauth_token_secret')
    })

if __name__ == '__main__':
    app.run(debug=True)
