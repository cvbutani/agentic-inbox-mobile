# Privacy Policy

**Agentic Inbox** ("the App") is developed by Chirag Butani ("I", "me"). This policy explains what data the App accesses, where it goes, and what control you have over it.

**Effective date:** August 12, 2026

## 1. What the App is

Agentic Inbox is a native Android and iOS email client. It is **bring-your-own-backend**: it does not connect to any email provider or server I operate. Instead, during setup you enter the URL and access credentials of a [`cloudflare/agentic-inbox`](https://github.com/cloudflare/agentic-inbox) Cloudflare Worker that **you** deploy and control. All of your email data lives on that Worker and whatever mail provider it is configured to connect to — not on any server belonging to me.

I do not operate a backend, and the App does not send any data to me. I have no ability to see your email, credentials, or usage of the App.

## 2. Data the App accesses

### 2.1 Your Worker credentials
During setup you provide a Worker URL and a Cloudflare Access service token (a Client ID and Client Secret). These are sent as HTTP headers on every request the App makes to your Worker, so your Worker can authenticate the request. I never receive these values myself.

### 2.2 Your email data
Once connected, the App requests your mailboxes, folders, messages, and attachments from your Worker and displays them. When you compose, reply, or forward a message, that content is sent to your Worker to be delivered. This traffic goes directly from your device to the Worker URL you configured — it does not pass through, or get logged by, any server I operate.

### 2.3 Data stored on your device
To work offline and load quickly, the App keeps a local cache on your device:

- **Credentials** (Worker URL, Client ID, Client Secret) are stored using platform-level secure storage (Android Keystore-backed storage / iOS Keychain).
- **Email metadata and content** — subjects, senders, recipients, dates, read/starred status, message snippets, and mailbox/folder lists — is cached in a local database on your device so the App can work offline.
- **Drafts you write** are stored locally, in full, until they are sent or discarded.
- **Attachments** you open or download are cached as files in the App's storage area on your device.

This local database is **not separately encrypted beyond your device's own disk encryption** (standard on modern Android and iOS). Anyone with access to an unlocked, unencrypted, or compromised device could potentially access this cache. Uninstalling the App removes this local data.

### 2.4 What I do *not* collect
The App does not include analytics, crash reporting, advertising, or tracking SDKs of any kind. I do not collect your name, email address, device identifiers, location, contacts, camera/photo access, or usage statistics. The App does not use push notifications. The App does not send your email content to any AI/language-model service — despite the name, the current mobile App has no AI features.

## 3. Third parties

The only third party involved is the Cloudflare Worker backend **you** deploy and configure, and whatever email provider you connect that Worker to. That backend is outside my control; its own data handling is governed by Cloudflare's terms and by however you've configured your Worker and mail provider, not by me. I recommend reviewing [Cloudflare's privacy policy](https://www.cloudflare.com/privacypolicy/) and the terms of whatever mail provider your Worker connects to.

I do not share, sell, or have access to any data associated with your use of the App.

## 4. Data retention and deletion

- **Local device data** (cache, drafts, attachments, stored credentials) is deleted when you uninstall the App, or can be cleared via your device's app-storage settings.
- **Data on your Worker/mail provider** is controlled entirely by you and is not something I can access, retain, or delete on your behalf. Manage or delete it through your Worker and mail provider directly.

## 5. Children's privacy

The App is not directed at children and requires deploying and configuring your own backend infrastructure, which is not something intended for use by children. I do not knowingly collect data from children.

## 6. Permissions

On Android, the App requests only the Internet permission, needed to reach the Worker URL you configure. On iOS, the App requests no special device permissions (camera, contacts, location, photos, etc.).

## 7. Your choices

Because I don't collect or hold your data, there is no account of yours for me to export or delete. You control your data by:

- Choosing what Worker/backend to connect the App to.
- Clearing the App's local cache or uninstalling it.
- Managing data retention on your own Worker and mail provider.

## 8. Changes to this policy

If I materially change what the App accesses or how it behaves, I will update this policy and revise the effective date above.

## 9. Contact

Questions about this policy or the App can be sent to **contact@sonicstarsolutions.com**.
