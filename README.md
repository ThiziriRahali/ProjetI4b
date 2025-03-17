# 🐸 Projet Frogger - INFO4B

Bienvenue dans notre projet **Frogger**, réalisé dans le cadre de l'UE *Principes des Systèmes d’Exploitation* du semestre 4.  
Ce projet a pour but de recréer et d'étendre le célèbre jeu d'arcade des années 80 en y ajoutant des fonctionnalités modernes et un mode multijoueur.

## 📌 Objectifs du projet

Notre implémentation de Frogger repose sur les principes fondamentaux des systèmes d'exploitation et comprend :

- 🔗 **Mode collaboratif en réseau** :
  - Jouez en équipe pour sauver un maximum de grenouilles.
  - Mode compétitif avec des joueurs perturbateurs utilisant une grenouille carnivore.

- 🏆 **Mode compétition en réseau** :
  - Victoire selon le nombre de grenouilles sauvées.
  - Jeu chronométré où le meilleur score l’emporte.

- 🐊 **Éléments de gameplay étendus** :
  - Présence d’alligators, de serpents et de tondeuses pour plus de difficulté.
  - Classement des joueurs en fonction des parties gagnées et du niveau atteint.

- 🎮 **Architecture modulaire** :
  - Serveur multi-clients et multi-parties.
  - Adaptation dynamique de la difficulté en fonction du classement des joueurs.

## 🚀 Technologies utilisées

- **Langage** : Java (SE 17+)
- **Frameworks/Bibliothèques** : JavaFX ou JCurses pour l’interface
- **Système cible** : GNU/Linux (Debian ≥ 12)
- **Gestion de version** : Git/GitHub

## 🔧 Installation et Exécution

### 1️⃣ Cloner le projet

```bash
git clone https://github.com/ThiziriRahali/ProjetI4b.git
cd ProjetI4b
```

### 2️⃣ Compiler le projet

```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
```

### 3️⃣ Lancer le serveur

```bash 
java -cp bin server.Server
```

### 4️⃣ Lancer un client

```bash
java -cp bin client.Client
```

## 📜 Organisation du code

- `src/server/` : Gestion du serveur de jeu.
- `src/client/` : Implémentation du client joueur.
- `src/game/` : Logique du jeu (mécanismes, règles, obstacles).
- `src/utils/` : Classes utilitaires et configuration.

## 📢 Contributeurs

👨‍💻 **RAHALI Thiziri**  
👨‍💻 **THIEBLEMONT Jérémy**  

## 📄 Licence

Projet académique - Licence non spécifiée.
