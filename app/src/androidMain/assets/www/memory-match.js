/**
 * KidZone Memory Match Game
 * Integrates with KidZoneGame engine for scoring, high scores, and ads.
 */

document.addEventListener('DOMContentLoaded', () => {
    const gameId = 'memory-match';
    const game = new KidZoneGame({
        id: gameId,
        onScoreUpdate: (score) => {
            console.log(`[MemoryMatch] Score updated: ${score}`);
            // You can update a global score display here if needed
        }
    });

    const translations = {
        en: {
            score: "Score",
            timer: "Time",
            start: "Start Game",
            go:   "Go!",
            win:  "You Win!",
            over: "Time's Up!",
            again:"Play Again"
        },
        uz: {
            score: "Ball",
            timer: "Vaqt",
            start: "Boshlash",
            go:   "Boshlang!",
            win:  "Yutdingiz!",
            over: "Vaqt tugadi!",
            again:"Qayta o'ynash"
        },
        ru: {
            score: "Очки",
            timer: "Время",
            start: "Начать",
            go:   "Вперёд!",
            win:  "Ты выиграл!",
            over: "Время вышло!",
            again:"Ещё раз"
        }
    };
    const t = game.getTranslations(translations);

    const cardEmojis = [
        '🍎', '🍌', '🍇', '🍓', '🍍', '🥝', '🍊', '🍋',
        '⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🏉', '🎱'
    ]; // Increased to 16 unique emojis for 32 cards
    const numPairs = 8; // Number of pairs to use for the game
    let cards = [];
    let flippedCards = [];
    let matchedPairs = 0;
    let score = 0;
    let timer;
    let timeLeft = 60; // seconds
    let gameActive = false;

    const gameBoard = document.getElementById('game-board');
    const scoreDisplay = document.getElementById('score-display');
    const timerDisplay = document.getElementById('timer-display');
    const startButton = document.getElementById('start-button');
    const messageDisplay = document.getElementById('message-display');

    function initGame() {
        gameBoard.innerHTML = '';
        flippedCards = [];
        matchedPairs = 0;
        score = 0;
        timeLeft = 60;
        gameActive = false;
        clearInterval(timer);
        updateScoreDisplay();
        updateTimerDisplay();
        timerDisplay.classList.remove('timer-warning');
        messageDisplay.textContent = '';
        startButton.textContent = t.start;
        startButton.style.display = 'block';

        // Inject Mute Button Styles if not exists
        if (!document.getElementById('mute-btn-styles')) {
            const style = document.createElement('style');
            style.id = 'mute-btn-styles';
            style.innerHTML = `
                #global-mute-btn {
                    position: fixed; top: 15px; right: 15px; z-index: 1000;
                    width: 45px; height: 45px; border: none; border-radius: 50%;
                    background: rgba(255, 255, 255, 0.9);
                    box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                    font-size: 22px; cursor: pointer;
                    display: flex; align-items: center; justify-content: center;
                    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                }
                #global-mute-btn:active { transform: scale(0.85); }
                #global-mute-btn.is-muted {
                    background: rgba(200, 200, 200, 0.9);
                    box-shadow: inset 0 2px 5px rgba(0,0,0,0.1);
                }
                .glow-effect {
                    animation: kzGlow 0.6s ease-out;
                }
                @keyframes kzGlow {
                    0% { box-shadow: 0 0 0 0 rgba(76,175,80,0.7); }
                    70% { box-shadow: 0 0 0 15px rgba(76,175,80,0); }
                    100% { box-shadow: 0 0 0 0 rgba(76,175,80,0); }
                }
                .timer-warning {
                    color: #E53935 !important;
                    animation: timerPulse 0.5s ease-in-out infinite alternate;
                }
                @keyframes timerPulse {
                    from { opacity: 1; } to { opacity: 0.5; }
                }
            `;
            document.head.appendChild(style);
        }

        // Add Global Mute Button if not exists
        if (!document.getElementById('global-mute-btn')) {
            const muteBtn = document.createElement('button');
            muteBtn.id = 'global-mute-btn';
            muteBtn.textContent = game.isMuted ? '🔇' : '🔊';
            muteBtn.classList.toggle('is-muted', game.isMuted);
            muteBtn.onclick = () => {
                game.isMuted = !game.isMuted;
                localStorage.setItem('kz-muted', game.isMuted);
                muteBtn.textContent = game.isMuted ? '🔇' : '🔊';
                muteBtn.classList.toggle('is-muted', game.isMuted);
            };
            document.body.appendChild(muteBtn);
        }

        // Select a subset of emojis for the current game
        const selectedEmojis = shuffleArray(cardEmojis).slice(0, numPairs);
        const gameCards = [...selectedEmojis, ...selectedEmojis]; // Duplicate for pairs
        cards = shuffleArray(gameCards);

        gameBoard.style.gridTemplateColumns = `repeat(${Math.sqrt(numPairs * 2)}, 1fr)`; // Dynamic grid

        cards.forEach((emoji, index) => {
            const cardElement = createCard(index, emoji);
            gameBoard.appendChild(cardElement);
        });
    }

    function createCard(id, emoji) {
        const card = document.createElement('div');
        card.classList.add('card');
        card.dataset.id = id;
        card.dataset.emoji = emoji;

        const front = document.createElement('div');
        front.classList.add('front');
        front.textContent = emoji;

        const back = document.createElement('div');
        back.classList.add('back');

        card.appendChild(front);
        card.appendChild(back);

        card.addEventListener('click', () => flipCard(card));
        return card;
    }

    function flipCard(card) {
        if (!gameActive || flippedCards.length === 2 || card.classList.contains('flipped') || card.classList.contains('matched')) {
            return;
        }

        card.classList.add('flipped');
        flippedCards.push(card);
        game.vibrate(20); // Short vibration on flip
        game.playSound('flip');

        if (flippedCards.length === 2) {
            setTimeout(checkForMatch, 800);
        }
    }

    function checkForMatch() {
        const [card1, card2] = flippedCards;
        const emoji1 = card1.dataset.emoji;
        const emoji2 = card2.dataset.emoji;

        if (emoji1 === emoji2) {
            // Match!
            card1.classList.add('matched');
            card2.classList.add('matched');
            card1.classList.add('glow-effect');
            card2.classList.add('glow-effect');
            matchedPairs++;
            score += 100; // Award points for a match
            game.vibrate(100); // Longer vibration for a match
            game.playSound('match');
            updateScoreDisplay();

            if (matchedPairs === numPairs) {
                game.playSound('win');
                endGame(true); // All cards matched
            }
        } else {
            // Mismatch
            card1.classList.remove('flipped');
            card2.classList.remove('flipped');
            game.playSound('mismatch');
            game.vibrate(50); // Medium vibration for mismatch
        }
        flippedCards = [];
    }

    function updateScoreDisplay() {
        scoreDisplay.textContent = `${t.score}: ${score}`;
    }

    function updateTimerDisplay() {
        timerDisplay.textContent = `${t.timer}: ${timeLeft}s`;
    }

    function startGame() {
        initGame(); // Reset game state
        gameActive = true;
        startButton.style.display = 'none';
        messageDisplay.textContent = t.go;

        timer = setInterval(() => {
            timeLeft--;
            updateTimerDisplay();
            if (timeLeft <= 10 && timeLeft > 0) {
                timerDisplay.classList.add('timer-warning');
            }
            if (timeLeft <= 0) {
                clearInterval(timer);
                endGame(false); // Time's up
            }
        }, 1000);
    }

    function endGame(win) {
        gameActive = false;
        clearInterval(timer);
        if (win) {
            if (window.KZL) KZL.nextLevel('memory-match');
            score += timeLeft * 10; // Bonus for remaining time
            messageDisplay.textContent = `${t.win} ${t.score}: ${score}`;
            game.spawnConfetti();
            game.screenShake();
        } else {
            messageDisplay.textContent = `${t.over} ${t.score}: ${score}`;
            game.playSound('gameover');
        }
        game.gameOver(score); // Report final score and trigger ad logic
        startButton.textContent = t.again;
        startButton.style.display = 'block';
    }

    function shuffleArray(array) {
        for (let i = array.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
        return array;
    }

    startButton.addEventListener('click', startGame);

    initGame(); // Initialize game on load
});