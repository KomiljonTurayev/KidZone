document.addEventListener('DOMContentLoaded', () => {
    const engine = new KidZoneGame({ id: 'tic-tac-toe' });
    
    const translations = {
        en: {
            thinking: "Robot is thinking...",
            yourTurn: "Your turn!",
            win: "You won! 🎉",
            draw: "Draw! 🤝",
            lose: "Robot won! 🤖"
        },
        uz: {
            thinking: "Robot o'ylamoqda...",
            yourTurn: "Sizning navbatingiz!",
            win: "Siz yutdingiz! 🎉",
            draw: "Durang! 🤝",
            lose: "Robot yutdi! 🤖"
        },
        ru: {
            thinking: "Робот думает...",
            yourTurn: "Ваш ход!",
            win: "Вы выиграли! 🎉",
            draw: "Ничья! 🤝",
            lose: "Робот выиграл! 🤖"
        }
    };
    const t = engine.getTranslations(translations);

    const boardElement = document.getElementById('board');
    const statusElement = document.getElementById('status');
    
    let board = ['', '', '', '', '', '', '', '', ''];
    let gameActive = true;
    const playerSign = '❌';
    const botSign = '⭕';
    statusElement.innerText = t.yourTurn;

    // Create Board
    for (let i = 0; i < 9; i++) {
        const cell = document.createElement('div');
        cell.classList.add('cell');
        cell.setAttribute('data-index', i);
        cell.addEventListener('click', handleCellClick);
        boardElement.appendChild(cell);
    }

    function handleCellClick(e) {
        const index = e.target.getAttribute('data-index');
        if (board[index] !== '' || !gameActive) return;

        makeMove(index, playerSign);
        
        if (checkWin(playerSign)) {
            engine.playSound('win');
            endGame(t.win, 50);
        } else if (board.every(cell => cell !== '')) {
            endGame(t.draw, 20);
        } else {
            gameActive = false; // Disable during bot move
            statusElement.innerText = t.thinking;
            setTimeout(botMove, 600);
        }
    }

    function botMove() {
        if (!gameActive && board.includes('')) {
            // Simple AI: tries to win, then block, then random
            let move = findBestMove(botSign) || findBestMove(playerSign) || getRandomMove();
            
            makeMove(move, botSign);

            if (checkWin(botSign)) {
                engine.playSound('lose');
                endGame(t.lose, 5);
            } else if (board.every(cell => cell !== '')) {
                engine.playSound('draw');
                endGame(t.draw, 20);
            } else {
                gameActive = true;
                statusElement.innerText = t.yourTurn;
            }
        }
    }

    function makeMove(index, sign) {
        board[index] = sign;
        const cell = boardElement.children[index];
        cell.innerText = sign;
        cell.classList.add('taken');
        engine.vibrate(30);
        engine.playSound('move');
    }

    function checkWin(sign) {
        const winPatterns = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8], // Rows
            [0, 3, 6], [1, 4, 7], [2, 5, 8], // Cols
            [0, 4, 8], [2, 4, 6]             // Diagonals
        ];
        return winPatterns.some(pattern => {
            if (pattern.every(index => board[index] === sign)) {
                pattern.forEach(idx => boardElement.children[idx].classList.add('win'));
                return true;
            }
            return false;
        });
    }

    function findBestMove(sign) {
        const winPatterns = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]];
        for (let pattern of winPatterns) {
            const vals = pattern.map(i => board[i]);
            if (vals.filter(v => v === sign).length === 2 && vals.filter(v => v === '').length === 1) {
                return pattern[vals.indexOf('')];
            }
        }
        return null;
    }

    function getRandomMove() {
        const available = board.map((v, i) => v === '' ? i : null).filter(v => v !== null);
        return available[Math.floor(Math.random() * available.length)];
    }

    function endGame(msg, score) {
        statusElement.innerText = msg;
        gameActive = false;
        engine.vibrate(100);
        
        if (msg === t.win) {
            engine.spawnConfetti();
            engine.screenShake();
        }

        // Engine'ga natijani hisobot qilish va reklamani boshqarish
        setTimeout(() => {
            engine.gameOver(score);
        }, 1000);
    }

    window.resetGame = function() {
        board = ['', '', '', '', '', '', '', '', ''];
        gameActive = true;
        statusElement.innerText = t.yourTurn;
        Array.from(boardElement.children).forEach(cell => {
            cell.innerText = '';
            cell.classList.remove('taken', 'win');
        });
    };
});