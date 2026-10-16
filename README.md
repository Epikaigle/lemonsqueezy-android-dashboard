# Lemon Dashboard 🍋

Application **Android native et personnelle** pour consulter les commandes, factures récurrentes et abonnements Lemon Squeezy. Kotlin, Compose, HTTPS et sauvegarde locale chiffrée.

## Installer sur ton Pixel 7 Pro

1. Ouvre **Actions → Android CI** sur ce dépôt.
2. Choisis le dernier build réussi et télécharge **lemon-dashboard-debug-apk** (artifact ZIP).
3. Décompresse puis installe l'APK sur ton Pixel 7 Pro. Autorise l'installation depuis cette source si Android le demande.
4. Crée une clé en mode **test** ou **live** dans les paramètres API Lemon Squeezy, puis saisis-la **uniquement dans l'application**.

Les APK de CI sont signées avec une clé de développement éphémère. Pour mettre à jour une version déjà installée sans la désinstaller, il faudra une **clé de signature persistante**, jamais committée, et un workflow release dédié.

## Fonctionnalités

- Accueil et graphique des ventes (périodes jour, mois, année et tout l'historique).
- Revenu enregistré depuis les commandes payées et les factures d'abonnement **non initiales** payées (anti-double comptage).
- Ventes par produit, nombre de commandes, montants remboursés et revenu après remboursements.
- Liste des abonnements et prochaine date de renouvellement.
- Sélection de boutique, inclusion ou non du mode test, affichage USD ou devise originale.
- Synchronisation manuelle, historique hors ligne, chiffrement Android Keystore.
- Guide de versements sans inventer de paiements confirmés.

## Limites connues

- Les montants affichés correspondent aux **ventes**, pas au solde ni aux virements réellement versés.
- Les renouvellements et ajustements de factures sont inclus, les factures initiales ne le sont pas pour éviter un double comptage. Cela reste une **définition de métrique à rapprocher du dashboard officiel**.
- L'API Lemon Squeezy publique ne documente pas un endpoint officiel de réconciliation exhaustive des payouts.
- Conversion historique USD fournie par Lemon Squeezy ; une conversion EUR non disponible n'est **jamais inventée**.
- Les abonnements n'exposent pas directement un prix récurrent fixe complet : pas de MRR estimé présenté comme confirmé.
- La clé API reste sur l'appareil, mais une application mobile ne peut offrir les mêmes garanties qu'un proxy serveur. Sécurise le téléphone, utilise une clé dédiée et révoque-la en cas de perte.

## Développement

JDK 17, Android SDK 37, AGP 9.2.0 et Gradle 9.4.1.

```bash
gradle testDebugUnitTest assembleDebug
```

Les sources sont sous `app/src/main/java/com/epikaigle/lemondashboard/`.

## Sécurité

Ne jamais publier une clé API, un token, des données clients, une copie des préférences ou une clé de signature GitHub. API HTTPS uniquement, cache chiffré, sauvegardes désactivées, aucun SDK de tracking.
