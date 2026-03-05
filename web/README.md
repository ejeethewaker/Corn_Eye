# CornEye — Web Admin Dashboard

React-based admin dashboard for the CornEye corn disease detection system.

## Features

- **Dashboard** — Total users, total scans, diseases detected, healthy scans with weekly/monthly trend percentages
- **Users** — List of all registered farmers with status badges and profile photos
- **User Profile** — Per-farmer detail: contact info, subscription, scan results
- **Notifications** — Real-time scan alerts and new-farmer notifications; click any card to mark as read
- **Admin Profile** — Admin account view

## Running locally

```bash
npm install
npm start        # http://localhost:3000
```

## Production build

```bash
npm run build
```

## Firebase config

Edit `src/firebase.js` with your project credentials. The app uses **Firebase Realtime Database** only (no Firestore).

## Routes

| Route | Component |
|---|---|
| `/` | Login |
| `/dashboard` | Dashboard |
| `/users` | Users list |
| `/user/:id` | User / Farmer detail |
| `/notifications` | Notifications feed |
| `/profile` | Admin profile |

All routes except `/` are protected — redirect to `/` if not authenticated.

## Authentication

Admin login is stored in `localStorage` (`adminLoggedIn: "true"`). No Firebase Auth is used for the admin panel.

---

See the root [README.md](../README.md) for full project documentation.


### Analyzing the Bundle Size

This section has moved here: [https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size](https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size)

### Making a Progressive Web App

This section has moved here: [https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app](https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app)

### Advanced Configuration

This section has moved here: [https://facebook.github.io/create-react-app/docs/advanced-configuration](https://facebook.github.io/create-react-app/docs/advanced-configuration)

### Deployment

This section has moved here: [https://facebook.github.io/create-react-app/docs/deployment](https://facebook.github.io/create-react-app/docs/deployment)

### `npm run build` fails to minify

This section has moved here: [https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify](https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify)
