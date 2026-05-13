import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  // These would be populated from your Firebase project settings
  apiKey: "YOUR_API_KEY",
  authDomain: "synapse-productivity.firebaseapp.com",
  projectId: "synapse-productivity",
  storageBucket: "synapse-productivity.appspot.com",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
