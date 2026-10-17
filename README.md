# Lemon Dashboard 🍋

Tableau de bord personnel Android 17 pour Lemon Squeezy, conçu pour **Pixel 7 Pro**.

Application Kotlin + Jetpack Compose. Le tableau de bord consulte l'API Lemon Squeezy **directement** ; les notifications push optionnelles passent par un relais Cloudflare distinct. La clé API est saisie **uniquement dans l'application**, chiffrée au repos par Android Keystore. Aucun secret n'est inclus dans le dépôt public.

## Fonctionnalités v1.4

- Ventes et renouvellements, graphiques par jour (heures), semaine, mois, année et historique complet.
- Notifications push optionnelles pour ventes, renouvellements et remboursements (Firebase + Worker sécurisé).
- Comparaison avec la période antérieure **sur la même durée écoulée**.
- Top produits, brut, remboursements et net après remboursement.
- Recherche et filtres : transactions payées, remboursées, non encaissées.
- Recherche, filtres et échéances pour les abonnements.
- Prix catalogue des abonnements fixes, dernières factures connues.
- Multi-boutiques, EUR indicatif / USD, données de test facultatives.
- Cache local chiffré et compressé, écriture atomique et migration du cache précédent ; actualisation explicite.
- Calendrier des versements annoncé comme *indicatif*. Aucun solde ni virement confirmé inventé.

Les revenus affichés **ne sont pas** les versements bancaires nets.

## Sécurité et confidentialité

- **Aucune clé Lemon Squeezy n'est stockée dans GitHub.**
- Le jeton et l'historique JSON sont chiffrés sur le téléphone avec AES-GCM et Android Keystore.
- Backup Android désactivé, HTTPS exclusivement, captures d'écran bloquées.
- Le taux indicatif EUR provient de Frankfurter (requête sans jeton API Lemon Squeezy).
- Si le Pixel est perdu, révoquer la clé API depuis le compte Lemon Squeezy.
- Le dépôt peut être public ; **ne jamais committer** les keystores de signature, mots de passe, secrets, factures ou caches privés.

## Signature persistante : configuration GitHub Actions

La clé privée persistante est **générée hors GitHub**. Pour que les prochaines APK soient signées avec le même certificat, il faut renseigner les deux secrets ci-dessous **une seule fois** dans les paramètres GitHub du dépôt :
`Settings → Secrets and variables → Actions → New repository secret`.

| Secret | Valeur |
|---|---|
| `LEMON_KEYSTORE_B64` | Contenu base64 **intégral, sur une seule ligne**, du fichier `lemon-dashboard-release.jks` |
| `LEMON_SIGNING_PASSWORD` | Mot de passe du fichier keystore |

Les deux valeurs doivent venir des fichiers privés remis au propriétaire ; le dépôt ne les contient **jamais**.

Ensuite ouvrir **Actions → Signed Android release → Run workflow**. Le workflow :
1. Restaure temporairement la clé privée depuis le secret chiffré GitHub Actions.
2. Compile l'APK release, avec `versionCode=5`.
3. Vérifie la signature via `apksigner verify`.
4. Publie l'artefact `LemonDashboard-v1.4.0-persistent-signed`, téléchargeable depuis l'exécution.
5. Publie la release GitHub v1.4.0 avec l’APK signée et son SHA-256.
6. Supprime la copie temporaire du keystore.

La signature doit rester **strictement identique** pour pouvoir mettre à jour l'application sans la désinstaller. Conserver une sauvegarde du keystore et du mot de passe dans un endroit sûr. **Ne pas changer de keystore** sur une version ultérieure.

### Important si la v1.0 debug est installée

La v1.0 précédente était une APK **debug signée par une autre clé**. Android ne pourra pas la remplacer par cette première release persistante : il faut désinstaller la debug **une seule fois**, puis installer la nouvelle APK signée. Cela efface aussi son cache et sa clé API locale. Toutes les futures releases **signées avec le même keystore** pourront ensuite se mettre à jour directement.

## Compiler

Android Gradle Plugin 9.2.0 · Gradle 9.4.1 · JDK 17 · API 37.0.

```bash
gradle testDebugUnitTest assembleDebug
gradle assembleRelease
```

Sans les variables d'environnement `LEMON_KEYSTORE_PATH` et `LEMON_KEYSTORE_PASSWORD`, la variante release n'est pas signée et **ne doit pas être distribuée**.

```bash
export LEMON_KEYSTORE_PATH="/chemin/vers/lemon-dashboard-release.jks"
export LEMON_KEYSTORE_PASSWORD="mot-de-passe-prive"
gradle assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Les builds de contrôle sont produits par **Android CI**, les builds signés par **Signed Android release** après installation des deux secrets.

## Limitations connues

- L'API publique Lemon Squeezy ne fournit pas toutes les informations nécessaires pour réconcilier les versements bancaires : consulter la console officielle.
- Les factures initiales d'abonnement sont exclues du calcul distinct des renouvellements pour éviter de doubler la commande initiale. Vérifier les totaux sur un historique réel.
- EUR : conversion indicative au taux disponible, pas aux cours historiques des virements.
- Les formules d'abonnement variables peuvent ne pas avoir de prix catalogue fixe affichable.
- L'application est personnelle et en lecture seule, sans analyse automatisée côté serveur.
- Les tests unitaires et GitHub CI **ne remplacent pas** un test sur un Pixel 7 Pro avec une clé Lemon Squeezy valide.


## Notifications push (v1.4)

La version v1.4 ajoute **les notifications de ventes, renouvellements et remboursements**, reçues via Firebase Cloud Messaging même lorsque l'application Android est fermée. Les factures initiales d'abonnement ne déclenchent pas de doublon. Les webhooks sont authentifiés par HMAC-SHA256 ; les envois sont filtrés par boutique et dédoublonnés avec une base Cloudflare D1.

**Configuration externe obligatoire** : crée un projet Firebase Android et un Cloudflare Worker, configure les secrets de webhook et de notification, puis compile une **APK signée avec le fichier google-services.json** du projet Firebase. Le workflow de release exige désormais aussi le secret GitHub **FIREBASE_ANDROID_CONFIG_JSON**. Aucune clé privée Firebase ou API Lemon Squeezy n'est intégrée au dépôt public.

Une fois configurée et installée : **Réglages → Notifications instantanées** pour saisir l'URL HTTPS du relais et le secret d'appairage, autoriser les notifications Android et activer les alertes. L'application reste utilisable sans Firebase, mais les notifications restent désactivées.

**Guide intégral** : [Déployer le relais et activer les notifications](push-worker/README.md). Les notifications ne sont pas garanties à la seconde et doivent être testées sur le Pixel réel. La version actuelle sur GitHub **ne signifie pas que le relais a déjà été déployé ou que des alertes arrivent** : l'infrastructure externe reste à connecter.

## Historique de fiabilité v1.4

- Historique 100 000 transactions testé côté agrégation ; un historique réel complet n'a pas encore été confronté à un compte Lemon Squeezy.
- Conversion EUR indicative, basée sur le taux disponible, et non sur le taux historique de chaque paiement.
- Chargement réseau et cache sur des threads non UI pour éviter les blocages.
- Les abonnements annulés peuvent conserver une période de grâce jusqu'à leur date d'expiration.

## Synchronisation, remboursements et tests (v1.4)

- L'actualisation normale relit les 30 derniers jours puis fusionne avec le cache local. Un rapprochement **complet** de toutes les transactions a lieu au moins toutes les 24 heures, et peut être lancé depuis Réglages. Une modification d'une transaction plus ancienne que 30 jours n'apparaît donc pas nécessairement avant le prochain rapprochement complet.
- Un remboursement complet porte une date de remboursement dans l'API ; cette date est utilisée pour les graphiques et indicateurs. Les remboursements partiels peuvent être sans date : ils sont isolés dans la catégorie « Sans date » pour « Tout » plutôt que d'être attribués artificiellement à la période de l'achat.
- Les tests JVM valident ces cas et l'ancien format de cache. La suite instrumentée Compose teste la page de connexion sur émulateur.
- La publication de la release exige **les deux secrets de signature** déjà documentés. Elle n'est pas possible sans eux. Ne générez pas une nouvelle clé si la clé persistante existe déjà.
- Une validation réelle sur Pixel 7 Pro avec des données Lemon Squeezy reste indispensable.
