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

### 📊 Analyse & Comparaison (Audit)
- **Comparateur Différentiel (⇄)** : Permet de comparer deux scans pour identifier instantanément quels octets ont été modifiés (idéal pour traquer les changements de solde).
- **Candidat Solde (💰)** : Mise en évidence automatique du **Bloc 37**, souvent utilisé pour le stockage des crédits ou compteurs dans les systèmes de distributeurs.
- **Export Automatique** : Génère un rapport `.txt` complet dans le dossier `Downloads` à chaque scan.

### 💾 Sauvegarde & Restauration (Backup/Restore)
- **Base de données Room (v3)** : Stockage local persistant de tous vos scans, incluant les données binaires brutes (`rawData`).
- **Mode Restauration Sécurisé** : Permet de réécrire les données sauvegardées sur un badge physique.
- **Protection Anti-Brick** : L'application protège les zones sensibles (Bloc 0 et Trailers de sécurité) en les excluant de l'écriture pour éviter de rendre le badge inutilisable.

---

## 💡 Cas d'utilisation

### 1. Gestion de Crédit "Offline" (Ex: Machine à café, Cantine)
- **Scénario** : Vous remarquez que votre solde augmente ou diminue mystérieusement.
- **Usage** : Scannez le badge avant et après un achat. Utilisez la fonction **Compare** pour isoler le bloc contenant le montant. Si le système est "Offline", vous pouvez utiliser la fonction **Restore** pour récupérer un solde précédent sauvegardé.

### 2. Audit de Sécurité (Contrôle d'accès)
- **Scénario** : Tester la résistance d'un système de badges d'entreprise.
- **Usage** : Vérifier si les secteurs utilisent des clés d'usine par défaut (`FFFFFFFFFFFF`) ou si les données sont chiffrées. Identifier les systèmes vulnérables basés uniquement sur l'UID (Bloc 0).

### 3. Maintenance Industrielle & IoT
- **Scénario** : Accéder aux informations de configuration stockées dans des tags fixés sur des machines.
- **Usage** : Lire les données NDEF ou les blocs bruts pour extraire des numéros de série ou des dates de dernière révision.

### 4. Développement & Débogage
- **Scénario** : Vous développez une solution NFC et devez vérifier le contenu binaire réel écrit sur vos puces.
- **Usage** : Un outil simple et portable pour valider vos algorithmes d'écriture.

---

## 🛠 Guide d'utilisation rapide

1. **Scan Initial** :
   - Allez dans **Home** > **Start Scanning**.
   - Maintenez le badge immobile jusqu'au signal sonore/vibration.
2. **Analyse** :
   - Allez dans **History**.
   - Cliquez sur `⇄` sur votre scan de référence.
   - Cliquez sur un autre scan pour voir les différences (en rouge).
3. **Restauration** :
   - Dans **History**, cliquez sur **Restore** sur la sauvegarde souhaitée (le bouton devient orange **Cancel**).
   - Retournez dans **Home**, activez le scan et présentez le badge.

---

## 🔒 Confidentialité & Sécurité
- Toutes les données sont stockées **uniquement en local** dans la base de données SQLite de l'application.
- Aucune donnée n'est transmise vers des serveurs tiers.
- L'utilisation de cet outil doit se faire dans le respect des conditions d'utilisation des systèmes que vous auditez.

---
*Développé avec ❤️ pour l'expertise NFC sur Android.*
