# NFCScanner Expert

Une application Android de pointe développée en **Kotlin** et **Jetpack Compose**, conçue pour l'analyse, la sauvegarde et la restauration de tags NFC, avec une expertise particulière sur la technologie **Mifare Classic**.

## 🚀 Vue d'ensemble

NFCScanner transforme votre smartphone en un véritable laboratoire NFC. Contrairement aux scanners basiques, cet outil est capable de réaliser des audits profonds sur les secteurs protégés, de mémoriser l'état binaire complet d'un badge et de restaurer des données en cas de besoin.

---

## ✨ Fonctionnalités Clés

### 🔍 Expertise Mifare Classic (1K)
- **Brute-force par Dictionnaire** : Utilise des dictionnaires de clés intégrés (`std.keys`, `hotel-std.keys`, `extended-std.keys`) pour déverrouiller les secteurs protégés.
- **Auto-Découverte de Clés** : Identifie et affiche la clé exacte (A ou B) ayant permis l'accès à chaque secteur (ex: détection de clés propriétaires comme "AZTEKM").
- **Lecture Hexadécimale & ASCII** : Affiche le contenu brut de chaque bloc avec une tentative de traduction en texte lisible.
- **Structure Organisée** : Visualisation claire des données regroupées par secteurs (Secteurs 0 à 15).

### ⚡ Optimisation de Performance
- **Cache Dynamique de Clés** : Mémorise les clés fonctionnelles pendant le scan pour accélérer la lecture des secteurs suivants. Réduit le temps de scan de 20 secondes à moins de 2 secondes pour les badges connus.
- **Robustesse NFC** : Timeout étendu à 5000ms et reconnexion automatique pour stabiliser la lecture même en cas de légers mouvements du badge.

### 📊 Audit & Analyse
- **Comparateur Différentiel (⇄)** : Permet de comparer deux scans pour identifier instantanément quels octets ont été modifiés (idéal pour traquer les changements de solde).
- **Candidat Solde (💰)** : Mise en évidence automatique du **Bloc 37**, souvent utilisé pour le stockage des crédits ou compteurs.
- **Interface Ergonomique** : Historique compact avec **déploiement au clic** pour une lecture claire des informations détaillées.

### 💾 Sauvegarde, Import & Restauration
- **Persistance Room (v3)** : Stockage local de tous vos scans, incluant les données binaires brutes (`rawData`).
- **Export & Import TXT Sécurisé** : Génère un rapport `.txt` dans le dossier `Downloads`. Ce fichier contient une section de données internes permettant d'**importer** à nouveau un scan dans l'application après une réinstallation.
- **Mode Restauration Sécurisé** : Permet de réécrire les données sauvegardées sur un badge physique.
- **Protection Anti-Brick** : Exclusion automatique des zones sensibles (Bloc 0 et Trailers de sécurité) lors de la restauration.

---

## 💡 Cas d'utilisation

### 1. Gestion de Crédit "Offline" (Ex: Machine à café, Cantine)
- **Usage** : Scannez le badge avant et après un achat. Utilisez la fonction **Compare** pour isoler le bloc contenant le montant. Si le système est "Offline", vous pouvez utiliser la fonction **Restore** pour récupérer un solde précédent sauvegardé.

### 2. Audit de Sécurité (Contrôle d'accès)
- **Usage** : Vérifier si les secteurs utilisent des clés d'usine par défaut (`FFFFFFFFFFFF`) ou si les données sont chiffrées. Identifier les systèmes vulnérables basés uniquement sur l'UID.

### 3. Archivage Permanent
- **Usage** : Utilisez la fonction **Import** pour restaurer vos bibliothèques de badges depuis vos fichiers texte sauvegardés dans `Downloads`, garantissant une survie des données même après suppression de l'application.

---

## 🛠 Guide d'utilisation rapide

1. **Scanner** : Allez dans **Home** > **Start Scanning**. Maintenez le badge immobile.
2. **Analyser** : Dans **History**, cliquez sur un badge pour voir ses détails ou sur `⇄` pour comparer.
3. **Restaurer** : Cliquez sur **Restore** sur un ancien scan, puis présentez le badge sur l'écran d'accueil.
4. **Importer** : Dans **History**, utilisez le bouton **Import** pour charger un fichier `.txt` généré précédemment.

---

## 🔒 Confidentialité & Sécurité
- Toutes les données sont stockées **uniquement en local** (SQLite + dossier Downloads).
- Aucune donnée n'est transmise vers des serveurs tiers.
- L'utilisation de cet outil doit se faire dans le respect des conditions d'utilisation des systèmes audités.

---
*Développé avec ❤️ pour l'expertise NFC sur Android.*
