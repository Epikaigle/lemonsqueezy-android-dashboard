# Lemon Dashboard 🍋

[![Release](https://img.shields.io/github/v/release/Epikaigle/lemonsqueezy-android-dashboard?color=%23f6cd61&logo=android&logoColor=black&label=Version)](https://github.com/Epikaigle/lemonsqueezy-android-dashboard/releases/latest)
[![Android CI](https://github.com/Epikaigle/lemonsqueezy-android-dashboard/actions/workflows/android.yml/badge.svg)](https://github.com/Epikaigle/lemonsqueezy-android-dashboard/actions/workflows/android.yml)
[![Push Worker Tests](https://github.com/Epikaigle/lemonsqueezy-android-dashboard/actions/workflows/push-worker.yml/badge.svg)](https://github.com/Epikaigle/lemonsqueezy-android-dashboard/actions/workflows/push-worker.yml)
[![Android](https://img.shields.io/badge/Android-13%20%E2%80%94%2017%20(API%2033--37)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

Application Android moderne et sécurisée, développée en **Kotlin** avec **Jetpack Compose (Material 3)**, permettant aux créateurs, développeurs et éditeurs de logiciels SaaS de suivre en temps réel leurs ventes, MRR, licences et abonnements **Lemon Squeezy**, avec alertes push instantanées et mises à jour in-app intégrées.

---

## 🏛️ Architecture et Structure du Répertoire

Le projet est conçu selon une architecture modulaire propre, scindant la couche d'accès aux données, l'analytique SaaS, l'interface graphique Compose et les services d'infrastructure Cloudflare Edge.

```
lemonsqueezy-android-dashboard/
├── .github/
│   └── workflows/
│       ├── android.yml            # CI tests unitaires, Android Lint & build APK
│       ├── android-ui.yml         # Tests d'interface automatisés sur émulateur Pixel
│       ├── push-worker.yml        # Tests unitaires Jest du microservice de notification
│       └── release.yml            # Pipeline de signature persistante & publication GitHub Release
├── app/
│   ├── build.gradle.kts           # Configuration Gradle Android, dépendances & versions
│   ├── google-services.json       # Déclaration client Firebase Cloud Messaging
│   ├── proguard-rules.pro         # Règles de minification R8/ProGuard
│   └── src/
│       ├── androidTest/           # Tests UI instrumentés Compose
│       ├── test/                  # Tests unitaires analytique, mappers & calculs
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/epikaigle/lemondashboard/
│           │   ├── Analytics.kt          # Calculs MRR/ARR, Churn, LTV, fenêtres temporelles
│           │   ├── AppUpdateManager.kt    # Détection & installation in-app des APK GitHub
│           │   ├── Components.kt          # Composants UI réutilisables (cartes, badges, modals)
│           │   ├── DashboardViewModel.kt  # StateFlow, cycle de vie et coordination réseau
│           │   ├── FilterChips.kt         # Barres de filtrage multi-boutiques et périodes
│           │   ├── LemonApi.kt            # Client Retrofit & API REST Lemon Squeezy
│           │   ├── LemonMapper.kt         # Mappage JSON:API vers modèles de domaine typés
│           │   ├── LoginScreen.kt         # Écran de connexion & guidage clé API
│           │   ├── MainActivity.kt        # Point d'entrée Android & Scaffold de navigation
│           │   ├── Models.kt              # Modèles de données (Commandes, Abonnements, Licences)
│           │   ├── OverviewScreen.kt      # Tableau de bord principal, KPIs SaaS & graphiques
│           │   ├── PayoutScreen.kt        # Échéancier indicatif et étapes des versements
│           │   ├── PushNotifications.kt   # Service FCM et synchronisation Cloudflare Worker
│           │   ├── SalesScreen.kt         # Liste, filtres avancés et détails des ventes
│           │   ├── SecureStore.kt         # Chiffrement AES-GCM matériel via Android Keystore
│           │   ├── SettingsScreen.kt      # Configuration, push, mises à jour, simulateur MoR
│           │   ├── SubscriptionsScreen.kt # Gestion abonnements récurrents & clés de licence SaaS
│           │   └── Theme.kt               # Thème Material 3 (palette sombre Lemon, typographie)
│           └── res/                      # Icônes vectorielles, chaînes et provider XML
├── docs/
│   └── index.html                 # Page de présentation officielle GitHub Pages
├── keystore/
│   └── release.jks                # Keystore de signature persistant
├── push-worker/
│   ├── src/index.js               # Relais Cloudflare Worker Edge (HMAC SHA-256 + FCM HTTP v1)
│   ├── schema.sql                 # Schéma D1 SQLite pour appairage chiffré des appareils
│   ├── wrangler.toml              # Configuration déploiement Cloudflare Workers
│   └── test/                      # Tests Jest de validation du webhook et des signatures
├── gradle/
│   └── wrapper/                   # Wrapper Gradle 9.4.1
├── build.gradle.kts               # Configuration racine du projet
├── settings.gradle.kts            # Déclaration des modules Gradle
└── README.md                      # Documentation complète
```

---

## 🚀 Fonctionnalités Détaillées

### 1. Tableau de bord SaaS & Analytique en temps réel
- **Métriques SaaS avancées** : MRR (*Monthly Recurring Revenue*), ARR, taux d'attrition mensuel (*Churn Rate*), panier moyen (*AOV*), revenu moyen par abonné (*ARPU*), valeur vie client (*LTV*).
- **Indicateur de santé commerçant (*Merchant Health*)** : Surveillance continue du taux de remboursement avec seuils d'alerte stricts (Good < 1%, Warning 1-2%, Critical > 2%).
- **Graphiques interactifs de tendance** : Visualisation par heure, jour, semaine, mois ou historique complet avec sélection de points au doigt.
- **Comparaison à période équivalente** : Calcul d'évolution calculé sur la durée écoulée exacte (ex. comparer les 15 premiers jours du mois courant aux 15 premiers jours du mois précédent).

### 2. Gestion des Licences Logicielles SaaS
- Consultation en direct des **clés de licence** émises par Lemon Squeezy pour vos applications et logiciels.
- Suivi du quota d'utilisation (`activations_count` / `activation_limit`).
- Gestion et inspection des **instances activées** (nom de machine, identifiant d'appareil, date d'activation).
- Possibilité d'activer ou désactiver des instances directement depuis l'application via l'API officielle.

### 3. Ventes, Filtres & Détails des Transactions
- Recherche plein texte instantanée par nom de client, adresse e-mail ou intitulé de produit.
- Filtres rapides : *Toutes, Payées, Remboursées, Échouées / Non encaissées*.
- Feuille détaillée de commande (*BottomSheet*) avec détail des taxes, montants bruts, montants nets et conversion USD/EUR en direct via Frankfurter.

### 4. Système de Mise à Jour In-App (GitHub Releases)
- Détection automatique et manuelle des nouvelles versions publiées sur le dépôt GitHub via l'API Releases.
- Téléchargement sécurisé du fichier `.apk` avec barre de progression en temps réel.
- Déclenchement transparent du gestionnaire d'installation natif Android (`PackageInstaller` / `FileProvider`) sans nécessiter d'accès au Google Play Store.

### 5. Notifications Push Instantanées (Cloudflare Edge + FCM)
- Réception d'alertes push pour chaque événement Lemon Squeezy :
  - 💰 *Nouvelle vente réalisée*
  - 🔄 *Renouvellement d'abonnement*
  - ⚠️ *Remboursement ou litige émis*
- Microservice **Cloudflare Worker** ultra-léger avec base de données **Cloudflare D1** pour stocker les jetons d'appareils de façon sécurisée.
- Authentification mutuelle par signature HMAC SHA-256 entre Lemon Squeezy, le Worker et l'appareil Android.

### 6. Outils Commerçant & Finance
- **Simulateur de frais Merchant of Record (MoR)** : Calculateur interactif intégrant le barème Lemon Squeezy (5% + 0,50 $ par transaction), la TVA/Sales Tax estimée et la réserve de sécurité.
- **Export comptable universel CSV** : Génération et partage d'un export CSV complet de vos transactions et abonnements via la feuille de partage native Android.
- **Calendrier des versements indicatif** : Estimation des prochains versements bancaires basée sur les dates de cycle Lemon Squeezy.

---

## 🔒 Sécurité et Confidentialité

- **Zéro clé sur GitHub** : Aucune clé API, aucun secret d'authentification ni aucun mot de passe de signature n'est commité dans le dépôt public.
- **Chiffrement matériel Keystore** : Le jeton API Lemon Squeezy et le cache hors-ligne sont chiffrés au repos via **AES-256-GCM** en utilisant la clé matérielle `MasterKey` de l'Android Keystore (`EncryptedSharedPreferences`).
- **Protection contre les captures d'écran** : Activation automatique de `FLAG_SECURE` sur les vues sensibles pour empêcher la capture d'écran et la lecture dans le sélecteur de tâches.
- **Isolation réseau stricte** : Communication directe en HTTPS TLS 1.3 avec `api.lemonsqueezy.com` ; aucun intermédiaire tiers n'a accès à vos données financières ou clients.

---

## 🔑 Signature Persistante & Clé Release

Pour garantir que l'application puisse être mise à jour à l'infini (notamment via le système de mise à jour in-app) sans jamais nécessiter de désinstallation, toutes les versions officielles sont signées avec une clé privée unique et immuable.

- **Fichier Keystore** : `keystore/release.jks`
- **Alias** : `lemon-dashboard-release`
- **Empreinte SHA-256 du certificat** :
  ```
  e5eee894f30aa766d9d6bc9a3901c7d955172dd45ed282d331e4e83e8152f908
  ```

### Vérification de l'APK installée
Vous pouvez vérifier à tout moment la validité de la signature avec l'utilitaire `apksigner` :
```bash
apksigner verify --verbose --print-certs LemonDashboard-v1.5.3.apk
```

---

## 🛠️ Compilation & Développement Local

### Prérequis
- **JDK 17** (Temurin ou OpenJDK)
- **Android SDK** API 37 (`platforms;android-37.0`, `build-tools;36.0.0`)
- **Gradle 9.4.1** (fourni via le wrapper `./gradlew`)

### Commandes utiles

```bash
# Lancer les tests unitaires
./gradlew test --no-daemon && ./gradlew --stop

# Vérifier la qualité du code avec Android Lint
./gradlew lintDebug --no-daemon && ./gradlew --stop

# Compiler l'APK Debug
./gradlew assembleDebug --no-daemon && ./gradlew --stop

# Compiler l'APK Release signée (nécessite les variables d'environnement)
export LEMON_KEYSTORE_PATH="keystore/release.jks"
export LEMON_KEYSTORE_PASSWORD="votre-mot-de-passe"
./gradlew assembleRelease --no-daemon && ./gradlew --stop
```

---

## 🌐 Déploiement du Worker de Notification (Optionnel)

Si vous souhaitez héberger votre propre relais de notifications push sur Cloudflare Workers :

```bash
cd push-worker
npm install
npm test

# Initialiser la base D1
npx wrangler d1 create lemon-push-db
npx wrangler d1 execute lemon-push-db --file=schema.sql

# Déployer sur Cloudflare Edge
npx wrangler deploy
```

---

## 📄 Licence & Clause de non-responsabilité

Ce projet est un outil tiers indépendant et n'est pas affilié, sponsorisé ou approuvé par Lemon Squeezy. Les estimations financières sont fournies à titre indicatif ; veuillez vous référer à votre tableau de bord officiel Lemon Squeezy pour vos relevés fiscaux et comptables officiels.
