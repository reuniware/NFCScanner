# NFCScanner

Une application Android simple et moderne développée en **Kotlin** et **Jetpack Compose** pour scanner les tags NFC, afficher leurs informations techniques et conserver un historique des détections.

## 🚀 Description du projet

NFCScanner permet d'utiliser le capteur NFC de votre smartphone pour lire les identifiants uniques (UID) et les technologies supportées par différents types de tags ou cartes NFC. L'application utilise **Room** pour stocker localement les données scannées, permettant de consulter l'historique même après redémarrage.

### Fonctionnalités clés :
- **Scan en temps réel** : Activation/Désactivation manuelle du mode lecture NFC.
- **Informations détaillées** : Extraction du numéro de série (UID), de la liste des technologies (NfcA, Mifare, etc.) et des métadonnées supplémentaires.
- **Historique Persistant** : Sauvegarde automatique de chaque détection dans une base de données locale.
- **Interface Moderne** : Navigation fluide entre l'écran d'accueil et l'historique grâce à Material 3.

## 🛠 Installation et Utilisation

1.  **Prérequis** : Un appareil Android équipé d'une puce NFC.
2.  **Lancement** : Ouvrez l'application et assurez-vous que le NFC est activé dans les paramètres de votre téléphone.
3.  **Scanner** :
    - Allez sur l'onglet **Home**.
    - Cliquez sur **Start Scanning**.
    - Approchez un tag NFC du dos de votre appareil.
    - Une notification (Toast) et une carte d'information confirmeront la lecture.
4.  **Historique** : Consultez l'onglet **History** pour voir la liste de tous les appareils détectés. Vous pouvez vider l'historique à tout moment via le bouton "Clear All".

## 💡 Cas d'utilisation et Contextes

Cet outil peut être utile dans de nombreux scénarios professionnels et personnels :

- **Gestion d'inventaire et Logistique** : Identifier rapidement des produits ou des bacs équipés de puces NFC pour le suivi de stock.
- **Contrôle d'Accès** : Vérifier le fonctionnement et l'UID des badges d'accès (bureaux, parkings) pour s'assurer de leur encodage correct.
- **Maintenance Industrielle** : Scanner des étiquettes NFC fixées sur des machines pour accéder instantanément à leur fiche technique ou journal de maintenance (via l'UID).
- **Configuration d'objets connectés (IoT)** : Simplifier l'appairage ou la configuration initiale de périphériques utilisant le NFC comme déclencheur.
- **Événementiel** : Gestion des entrées ou validation de bracelets connectés lors de conférences ou festivals.
- **Développement et Test** : Un outil simple pour les développeurs travaillant sur des solutions NFC afin de déboguer rapidement le contenu et le type de tag.

---
Développé avec ❤️ pour Android.
