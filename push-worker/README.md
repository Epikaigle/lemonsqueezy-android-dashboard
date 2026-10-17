# Notifications instantanées Lemon Dashboard (Firebase + Cloudflare)

Un webhook signé de Lemon Squeezy arrive dans un Cloudflare Worker, qui vérifie son HMAC-SHA256 et l'identifiant de la boutique, protège les reprises avec une base D1, puis transmet une **notification FCM** au Pixel. L'application Android peut rester fermée.

Le relais **ne reçoit jamais la clé API Lemon Squeezy**. Il ne conserve ni clients, ni adresses mail, ni détails de paiement : seulement le jeton FCM de ton téléphone et des empreintes techniques d'événements pour empêcher les doublons. Le payload reçu contient momentanément les données de Lemon Squeezy en mémoire, puis est jeté.

## 1. Firebase (sur ton compte)

1. Crée un projet dans [Firebase Console](https://console.firebase.google.com/) et ajoute une app Android **com.epikaigle.lemondashboard**.
2. Télécharge son fichier `google-services.json`. Dans GitHub, crée le secret Actions **FIREBASE_ANDROID_CONFIG_JSON** avec le **contenu intégral du fichier JSON**. Le fichier est volontairement exclu du dépôt.
3. Dans les paramètres Firebase / Service Accounts, génère une **clé privée de compte de service** pour l'envoi de messages FCM HTTP v1, puis conserve ce JSON hors GitHub. Vérifie que l'API Firebase Cloud Messaging HTTP v1 est activée pour le projet Google Cloud.
4. Note le **Project ID** Firebase, pas l'ID de l'application Android.

La **clé privée** du compte de service ne doit jamais être intégrée à l'application Android et doit rester un secret côté Worker. Le `google-services.json` du client contient des identifiants publics de configuration ; l'emploi d'un secret GitHub ici évite de publier même ces identifiants dans ton dépôt.

## 2. Cloudflare Worker + base D1

Depuis un terminal avec Node.js, dans `push-worker/` :

```bash
npm install
npx wrangler login
npx wrangler d1 create lemon-dashboard-push
```

Dans `wrangler.toml`, remplace **REPLACE_WITH_YOUR_D1_DATABASE_ID** par l'UUID renvoyé par Wrangler, **REPLACE_WITH_YOUR_FIREBASE_PROJECT_ID** par le Project ID de Firebase et **REPLACE_WITH_YOUR_LEMON_STORE_ID** par l'identifiant numérique de ta boutique Lemon Squeezy (plusieurs ID séparés par des virgules si nécessaire). **Ne publie jamais de secret dans ce fichier**.

```bash
npm run db
npx wrangler secret put LEMON_WEBHOOK_SECRET
npx wrangler secret put DEVICE_PAIRING_SECRET
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT_JSON
npm test
npm run deploy
```

- `LEMON_WEBHOOK_SECRET` : chaîne aléatoire de **6 à 40 caractères**, **strictement identique** au secret de webhook défini dans Lemon Squeezy.
- `DEVICE_PAIRING_SECRET` : **une chaîne aléatoire différente**, idéalement 32 caractères ou plus. Elle sera saisie **une seule fois dans l'application**. Ne la réutilise pas comme clé API.
- `FIREBASE_SERVICE_ACCOUNT_JSON` : le **JSON entier** du compte de service Firebase avec `private_key` et `project_id`. Entrer sa valeur de façon privée dans Wrangler.

Le déploiement indique l'adresse `https://…workers.dev`. Une visite de `/health` renvoie `{"ok":true}` même si des secrets manquent ; **ce test n'est pas une vérification de bout en bout**.

## 3. Webhook Lemon Squeezy

Dans [Lemon Squeezy → Settings → Webhooks](https://app.lemonsqueezy.com/settings/webhooks), crée un webhook :

- **URL** : `https://TON-WORKER.workers.dev/webhook`.
- **Secret** : valeur exacte de `LEMON_WEBHOOK_SECRET` (jamais le secret d'appairage).
- **Événements** : `order_created`, `order_refunded`, `subscription_payment_success`, `subscription_payment_refunded`.
- **Mode test** : désactivé dans le Worker par défaut ; pour des simulations, configure temporairement `ALLOW_TEST_MODE = "true"` et crée le webhook correspondant dans le mode test Lemon Squeezy.

Les factures initiales d'abonnement sont ignorées côté Worker pour ne pas doubler la notification de vente. Les remboursements partiels montrent le **montant cumulé remboursé** (pas le delta), conformément aux données renvoyées par le webhook.

## 4. Installer la version Android signée

Assure-toi d'avoir aussi les deux secrets de signature persistante documentés à la racine : `LEMON_KEYSTORE_B64` et `LEMON_SIGNING_PASSWORD`. Ne remplace pas ta clé de signature existante.

Déclenche **Actions → Signed Android release → Run workflow**, récupère l'APK **v1.4.0** depuis la release GitHub et installe-la sur ton Pixel 7 Pro. Cette version doit avoir été compilée avec le secret `FIREBASE_ANDROID_CONFIG_JSON`. Il n'est **pas** possible de recevoir les notifications depuis une ancienne APK dépourvue de la configuration Firebase.

Dans l'application : **Réglages → Notifications instantanées**, saisis l'URL du Worker **sans `/webhook` ni autre chemin**, puis `DEVICE_PAIRING_SECRET` et autorise les notifications Android. L'activation enregistre le jeton FCM dans D1 et conserve le secret sous Android Keystore. Utilise **Désactiver les alertes** pour effacer le jeton enregistré sur le relais. Si ton appareil est perdu, révoque `DEVICE_PAIRING_SECRET` et efface l'entrée D1 `devices`.

## Limites importantes

- Les notifications sont généralement reçues rapidement, **mais une réception instantanée à la seconde n'est pas garantie** (réseau, optimisation de la batterie, délais FCM).
- Si l'application est **forcée à l'arrêt par Android**, les notifications ne peuvent pas toujours être délivrées jusqu'à la prochaine ouverture.
- Un envoi FCM peut aboutir puis échouer avant l'accusé de réception dans D1 : un doublon reste exceptionnellement possible. Le relais utilise une clé d'idempotence et un bail d'envoi pour réduire ce risque.
- La désinstallation ou l'effacement des données locales ne garantit pas la suppression du jeton stocké sur le serveur lorsque l'appareil est hors ligne. Révoque alors manuellement le secret d'appairage et supprime la ligne D1.
- L'Android CI construit sans `google-services.json` et vérifie le code, mais **seules les releases signées avec ce fichier peuvent activer FCM**. Teste ensuite le flux avec un achat test et une notification réelle, sans données clients réelles.

Documentation : [webhooks Lemon Squeezy](https://docs.lemonsqueezy.com/help/webhooks), [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api), [Cloudflare Workers](https://developers.cloudflare.com/workers/).
