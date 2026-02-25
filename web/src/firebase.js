import { initializeApp } from 'firebase/app';
import { getDatabase } from 'firebase/database';

// TODO: Replace with your Firebase project configuration
const firebaseConfig = {
  apiKey: 'AIzaSyB7ylHAJgvdvnjwyvkzPsLH9gyPnGIGn20',
  authDomain: 'corneye-ec181.firebaseapp.com',
  databaseURL: 'https://corneye-ec181-default-rtdb.asia-southeast1.firebasedatabase.app',
  projectId: 'corneye-ec181',
  storageBucket: 'corneye-ec181.firebasestorage.app',
  messagingSenderId: '1007953451221',
  appId: '1:1007953451221:web:0bb01a7658ae11e6fbea5c',
};

const app = initializeApp(firebaseConfig);
const database = getDatabase(app);

export { database };
export default app;
