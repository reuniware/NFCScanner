# NFCScanner Expert

NFCScanner Expert est un outil Android professionnel conçu pour l'audit, l'analyse binaire et la restauration de tags NFC, avec une spécialisation poussée sur la technologie **Mifare Classic 1K**. 

Cette application transforme votre smartphone en un terminal d'expertise capable de percer les protections par clés et de suivre l'évolution des données bit à bit.

---

## 🛠 Guide Complet des Fonctionnalités

### 1. Scan Profond & Découverte de Clés
L'application ne se contente pas de lire l'ID ; elle tente d'ouvrir chaque secteur de la mémoire.
*   **Fonctionnement** : Utilise trois dictionnaires de clés intégrés (`std`, `hotel`, `extended`) pour tester les secteurs protégés.
*   **Auto-apprentissage** : Dès qu'une clé est trouvée (ex: la clé "AZTEKM"), elle est mise en cache pour accélérer instantanément la lecture des secteurs suivants et des futurs scans.
*   **Utilisation** : Allez sur l'onglet **Home**, cliquez sur **Start Scanning** et maintenez le badge immobile contre le téléphone (environ 10-15s pour un premier scan complet).

### 2. Analyse Binaire (Hexadécimal & ASCII)
Visualisez le contenu exact de la puce.
*   **Visualisation** : Les données sont regroupées par **Secteurs (0-15)**. Chaque secteur affiche ses 4 blocs de 16 octets.
*   **Double Format** : Affichage en Hexadécimal (pour les calculs) et en ASCII (pour lire les textes cachés).
*   **Utilisation** : Dans l'onglet **History**, cliquez sur un badge pour dérouler son contenu complet.

### 3. Comparateur Différentiel (⇄)
L'outil ultime pour comprendre comment une machine (ex: distributeur de café) modifie votre badge.
*   **Fonctionnement** : Compare deux scans bit à bit et met en évidence les changements.
*   **Focus Solde (💰)** : Le **Bloc 37** est automatiquement surveillé. S'il change, une icône de sac d'argent apparaît, indiquant un probable mouvement de crédit.
*   **Utilisation** : 
    1. Dans l'historique, cliquez sur l'icône `⇄` d'un scan (il devient bleu).
    2. Parcourez les autres scans : les différences apparaîtront automatiquement en rouge.

### 4. Sauvegarde & Importation Sécurisée (TXT)
Ne perdez jamais vos données, même si vous changez de téléphone.
*   **Export Auto** : Chaque scan génère un fichier `.txt` détaillé dans le dossier `Downloads` de votre Android.
*   **Format d'Archive** : Le fichier contient une section "INTERNAL RAW DATA" cryptique mais essentielle pour la reconstruction.
*   **Importation** : Cliquez sur le bouton **Import** dans l'historique pour recharger un scan depuis un ancien fichier texte.

### 5. Restauration de Données (Restore)
Réécrivez les informations sauvegardées sur un badge physique.
*   **Sécurité "Anti-Brick"** : L'application interdit l'écriture sur le Bloc 0 (ID) et les secteurs de sécurité (Trailers) pour éviter de détruire définitivement votre badge. Seules les **données** sont restaurées.
*   **Utilisation** :
    1. Cliquez sur **Restore** sur un scan de l'historique (le bouton devient orange **Cancel**).
    2. Retournez à l'accueil, lancez le scan et présentez le badge. L'application réinjectera les données originales.

### 6. Gestion Personnalisée (Friendly Name)
Identifiez vos badges plus facilement que par leur numéro de série.
*   **Utilisation** : Cliquez sur l'icône **crayon** ✏️ à côté d'un badge dans l'historique, saisissez un nom (ex: "Badge Cantine"), et validez avec la **coche** ✅.

---

## 💡 Cas d'Utilisation Pratiques

| Contexte | Action NFCScanner |
| :--- | :--- |
| **Machine à café / Lavomatique** | Comparer le badge avant/après achat pour identifier le bloc du solde. |
| **Contrôle d'accès** | Vérifier si le badge utilise des clés d'usine vulnérables (`FFFFFFFFFFFF`). |
| **Sécurité des données** | Détecter si des informations personnelles (nom, ID employé) sont stockées en clair. |
| **Sauvegarde de secours** | Créer une image de votre badge pour le restaurer s'il est corrompu par un lecteur défectueux. |

---

## ⚙️ Optimisations Techniques

- **Timeout Robuste** : Délai de réponse porté à 5000ms pour éviter les erreurs "Tag lost".
- **Multi-Thread** : Le scan s'effectue en arrière-plan pour ne pas bloquer l'interface.
- **Base de données Room v4** : Stockage local ultra-rapide et structuré.
- **Processing Lock** : Protection contre les déclenchements multiples lors d'un scan long.

---

## 🔒 Confidentialité
Toutes les opérations sont effectuées **localement**. Aucune donnée binaire, aucune clé et aucun identifiant ne sont transmis vers l'extérieur. Vos fichiers de sauvegarde restent dans votre dossier `Downloads` sous votre contrôle exclusif.

---
*Développé pour les passionnés de technologie NFC et les experts en sécurité.*
