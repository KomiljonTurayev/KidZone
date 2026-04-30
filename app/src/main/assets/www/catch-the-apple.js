/**
 * KidZone Catch the Apple Game
 * Integrates with KidZoneGame engine for scoring, high scores, and ads.
 */

document.addEventListener('DOMContentLoaded', () => {
    const gameId = 'catch-the-apple';
    const gameEngine = new KidZoneGame({
        id: gameId,
        onScoreUpdate: (score) => {
            console.log(`[CatchTheApple] Score updated: ${score}`);
            // No need to update global score here, game has its own display
        }
    });

    // Localization strings
    const translations = {
        en: {
            score: "Score",
            lives: "Lives",
            startGame: "Start Game",
            go: "Go!",
            gameOver: "Game Over!",
            playAgain: "Play Again",
            youWin: "You Win!"
        },
        uz: {
            score: "Ball",
            lives: "Hayot",
            startGame: "O'yinni boshlash",
            go: "Ketdik!",
            gameOver: "O'yin tugadi!",
            playAgain: "Qaytadan o'ynash",
            youWin: "Siz yutdingiz!"
        }
        // Add other languages as needed
    };
    const t = gameEngine.getTranslations(translations); // Get current language translations

    const canvas = document.getElementById('gameCanvas');
    const ctx = canvas.getContext('2d');
    const scoreDisplay = document.getElementById('score-display');
    const livesDisplay = document.getElementById('lives-display');
    const startButton = document.getElementById('start-button');
    const messageDisplay = document.getElementById('message-display');

    let player = {
        x: canvas.width / 2 - 30,
        y: canvas.height - 50,
        width: 60,
        height: 20,
        emoji: '🧺',
        isFlashing: false,
        flashColor: '',
        powerUpTimer: null
    };

    let apples = [];
    let floatingScores = [];
    let comboCount = 0;
    let score = 0;
    let lives = 3;
    let gameActive = false;
    let requestID;
    let appleSpawnInterval;
    let appleSpeed = 2; // Initial apple falling speed
    const APPLE_RADIUS = 15;
    // const MAX_APPLES = 10; // Max number of apples on screen at once - controlled by spawn interval

    function initGame() {
        apples = [];
        floatingScores = [];
        comboCount = 0;
        score = 0;
        lives = 3;
        gameActive = false;
        appleSpeed = 2;
        player.width = 60; // Reset size to normal
        if (player.powerUpTimer) clearTimeout(player.powerUpTimer);
        player.isFlashing = false;
        player.flashColor = '';
        player.x = canvas.width / 2 - player.width / 2; // Reset player position
        if (requestID) cancelAnimationFrame(requestID);
        clearInterval(appleSpawnInterval);
        updateScoreDisplay();
        updateLivesDisplay();
        messageDisplay.textContent = '';
        startButton.textContent = t.startGame;
        startButton.style.display = 'block';
        draw(); // Draw initial state
    }

    function startGame() {
        initGame(); // Reset everything before starting
        gameActive = true;
        startButton.style.display = 'none';
        messageDisplay.textContent = t.go;

        requestID = requestAnimationFrame(gameLoop);
        appleSpawnInterval = setInterval(spawnApple, 1500); // Spawn an apple every 1.5 seconds
    }

    function gameLoop() {
        if (!gameActive) return;
        update();
        draw();
        requestID = requestAnimationFrame(gameLoop);
    }

    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height); // Clear canvas

        // Draw floating scores
        drawFloatingScores();

        // Draw player (basket)
        ctx.font = `${player.width}px Arial`; // Use player width for emoji size
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(player.emoji, player.x + player.width / 2, player.y + player.height / 2);

        // Draw flash effect if hit by a bomb
        if (player.isFlashing) {
            ctx.fillStyle = player.flashColor;
            ctx.fillRect(player.x, player.y, player.width, player.height);
        }

        // Draw apples
        apples.forEach(apple => {
            ctx.font = `${APPLE_RADIUS * 2}px Arial`;
            ctx.fillText(apple.emoji, apple.x, apple.y); // apple.x, apple.y are centers
        });
    }

    function drawFloatingScores() {
        ctx.textAlign = 'center';
        floatingScores.forEach(fs => {
            ctx.globalAlpha = Math.min(1.0, fs.opacity);
            ctx.font = fs.font || 'bold 20px Arial';
            ctx.fillStyle = fs.color || '#22c55e'; // Default yashil
            ctx.fillText(fs.text, fs.x, fs.y);
        });
        ctx.globalAlpha = 1.0; // Shaffoflikni qayta tiklash
    }

    function update() {
        // Move apples
        for (let i = apples.length - 1; i >= 0; i--) {
            apples[i].y += appleSpeed;

            // Check for collision with player
            // Collision detection: apple bottom touches player top, and apple x is within player x range
            if (
                apples[i].y + APPLE_RADIUS >= player.y && // Apple bottom is at or below basket top
                apples[i].y - APPLE_RADIUS <= player.y + player.height && // Apple top is not past basket bottom
                apples[i].x + APPLE_RADIUS > player.x && // Apple right edge is past basket left edge
                apples[i].x - APPLE_RADIUS < player.x + player.width // Apple left edge is before basket right edge
            ) {
                // Item caught!
                if (apples[i].emoji === '💣') {
                    lives--;
                    comboCount = 0; // Reset combo on bomb
                    gameEngine.playSound('bomb');
                    gameEngine.screenShake(300, canvas); // Shake only the canvas
                    gameEngine.vibrate(200); // Strong vibrate for bomb
                    player.flashColor = 'rgba(255, 0, 0, 0.5)'; // Red for bomb
                    updateLivesDisplay();
                    
                    // Trigger flash effect
                    player.isFlashing = true;
                    setTimeout(() => { player.isFlashing = false; }, 300);

                    if (lives <= 0) endGame();
                } else {
                    const pointsToAdd = (player.width > 60) ? 2 : 1;
                    score += pointsToAdd;
                    comboCount++;
                    gameEngine.playSound('catch');
                    gameEngine.vibrate(50); 
                    
                    // Add floating score effect
                    floatingScores.push({
                        x: player.x + player.width / 2,
                        y: player.y - 10,
                        opacity: 1.0,
                        text: `+${pointsToAdd}`,
                        color: pointsToAdd === 2 ? '#f59e0b' : '#22c55e' // Multiplier uchun oltin rang
                    });

                    // Trigger Combo!
                    if (comboCount >= 5) {
                        score += 5; // Bonus score for combo
                        floatingScores.push({
                            x: player.x + player.width / 2,
                            y: player.y - 40,
                            opacity: 1.5,
                            text: '🔥 COMBO! 🔥',
                            color: '#f59e0b', // Oltin rang
                            font: 'bold 28px Arial'
                        });
                        gameEngine.vibrate(150);

                        // POWER-UP: Increase basket size for 3 seconds
                        player.width = 100;
                        if (player.powerUpTimer) clearTimeout(player.powerUpTimer);
                        player.powerUpTimer = setTimeout(() => {
                            if (gameActive) player.width = 60;
                        }, 3000);

                        comboCount = 0; // Reset for next combo
                    }

                    // Trigger green success flash
                    player.isFlashing = true;
                    player.flashColor = 'rgba(0, 255, 0, 0.5)'; // Green for success
                    setTimeout(() => { player.isFlashing = false; }, 200);

                    updateScoreDisplay();
                    // Increase difficulty
                    if (score > 0 && score % 5 === 0) {
                        appleSpeed += 0.3;
                    }
                }
                apples.splice(i, 1); 
            } else if (apples[i].y - APPLE_RADIUS > canvas.height) {
                // Apple missed!
                if (apples[i].emoji !== '💣') { // Missing a bomb is good!
                    lives--;
                    comboCount = 0; // Reset combo on miss
                    gameEngine.playSound('miss');
                    gameEngine.vibrate(100);
                    updateLivesDisplay();
                }
                apples.splice(i, 1); // Remove missed apple
                if (lives <= 0) {
                    endGame();
                }
            }
        }

        // Update floating scores
        for (let i = floatingScores.length - 1; i >= 0; i--) {
            floatingScores[i].y -= 1; // Yuqoriga ko'tarilish
            floatingScores[i].opacity -= 0.02; // Sekin yo'qolish
            if (floatingScores[i].opacity <= 0) {
                floatingScores.splice(i, 1);
            }
        }
    }

    function spawnApple() {
        const x = Math.random() * (canvas.width - APPLE_RADIUS * 2) + APPLE_RADIUS; // Center X
        const y = APPLE_RADIUS; // Center Y (starts just above canvas)
        
        // 20% chance to spawn a bomb, 80% fruit
        const isBomb = Math.random() < 0.2;
        const emoji = isBomb ? '💣' : ['🍎', '🍏', '🍐', '🍊', '🍓'][Math.floor(Math.random() * 5)];
        apples.push({ x, y, emoji });
    }

    function updateScoreDisplay() {
        scoreDisplay.textContent = `${t.score}: ${score}`;
    }

    function updateLivesDisplay() {
        livesDisplay.textContent = `${t.lives}: ${'❤️'.repeat(lives)}${'🖤'.repeat(Math.max(0, 3 - lives))}`;
    }

    function endGame() {
        gameActive = false;
        if (requestID) cancelAnimationFrame(requestID);
        clearInterval(appleSpawnInterval);
        gameEngine.playSound('gameover');
        messageDisplay.textContent = `${t.gameOver} ${t.score}: ${score}`;
        startButton.textContent = t.playAgain;
        startButton.style.display = 'block';

        if (score > 10) {
            gameEngine.spawnConfetti();
            gameEngine.screenShake(500, canvas); // Celebrate on the game area
        }

        gameEngine.gameOver(score); // Report final score and trigger ad logic
    }

    // Player movement (mouse/touch)
    canvas.addEventListener('mousemove', (e) => {
        if (gameActive) {
            const rect = canvas.getBoundingClientRect();
            const scaleX = canvas.width / rect.width;
            player.x = (e.clientX - rect.left) * scaleX - player.width / 2;
            // Keep player within canvas bounds
            if (player.x < 0) player.x = 0;
            if (player.x + player.width > canvas.width) player.x = canvas.width - player.width;
        }
    });

    canvas.addEventListener('touchmove', (e) => {
        if (gameActive && e.touches.length > 0) {
            e.preventDefault(); // Prevent scrolling
            const rect = canvas.getBoundingClientRect();
            const scaleX = canvas.width / rect.width;
            player.x = (e.touches[0].clientX - rect.left) * scaleX - player.width / 2;
            // Keep player within canvas bounds
            if (player.x < 0) player.x = 0;
            if (player.x + player.width > canvas.width) player.x = canvas.width - player.width;
        }
    }, { passive: false }); // passive: false to allow preventDefault

    startButton.addEventListener('click', startGame);

    initGame(); // Initialize game on load
});