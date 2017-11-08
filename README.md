# Itty Bitty Chat

A simple chat application inspired by WhatsApp but with the ability to login with your own social media handle. 

### Features
- Easy sign up using your Google, Facebook, and Twitter account along with a basic email option. 
- Friend lookup via email
- Notifications feature (friend requests as of now)
- Presence feature to let your friend know if you're online, or if not, the time you were last seen (WhatsApp-esque)
- Typing detection
- Read confirmation


### Notes
- Firebase UI handles the sign-in/sign-up interaction externally, and the transitions are a little off, but it's super handy.
- Facebook permissions for development purposes requires each dev to register their own hash-key.
- When a user loses their internet connection, Firebase handles all chats and activities locally and syncs up once they're back online. If they're disconnected for more than 60 seconds without a clean exit, the server-side Presence feature gets updated that they're no longer online.
- All messages are not encrypted (might try implementing that in the future), but passwords are all handled properly by Firebase UI (I hope).



### Compilation Notes
- Import the project into Android Studio as usual, but you'll need to set-up your machine's hash-keys for Firebase interaction. Android Studio automatically generates this for us in the Firebase Assistant (Tools -> Firebase -> Authentication -> Connect your app to Firebase)
