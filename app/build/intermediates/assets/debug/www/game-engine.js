/**
 * KidZone Game Engine (Shared Library)
 * Provides standardized APIs for all mini-games.
 */

class KidZoneGame {
    constructor(config = {}) {
        this.id = config.id || 'unknown';
        this.onScoreUpdate = config.onScoreUpdate || null;
        this.isMuted = localStorage.getItem("kz-muted") === "true";
        this.lang = this._getQueryParam('lang') || localStorage.getItem("kz-lang") || 'en';
        this.adCounter = parseInt(localStorage.getItem('kz-ad-cnt') || '0');

        this._initListeners();
    }

    _initListeners() {
        window.addEventListener('storage', (e) => {
            if (e.key === 'kz-muted') {
                this.isMuted = e.newValue === 'true';
                this.onMuteChange(this.isMuted);
            }
        });
        this._initAudio();
    }

    _initAudio() {
        this.audioCtx = null;
    }

    _ensureAudioCtx() {
        if (!this.audioCtx) {
            this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (this.audioCtx.state === 'suspended') {
            this.audioCtx.resume();
        }
    }

    playSound(type) {
        if (this.isMuted) return;
        this._ensureAudioCtx();

        const oscillator = this.audioCtx.createOscillator();
        const gainNode = this.audioCtx.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(this.audioCtx.destination);

        const now = this.audioCtx.currentTime;

        switch(type) {
            case 'move':
                oscillator.type = 'sine';
                oscillator.frequency.setValueAtTime(400, now);
                oscillator.frequency.exponentialRampToValueAtTime(600, now + 0.1);
                gainNode.gain.setValueAtTime(0.1, now);
                gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
                oscillator.start(now);
                oscillator.stop(now + 0.1);
                break;
            case 'win':
                oscillator.type = 'triangle';
                oscillator.frequency.setValueAtTime(523.25, now); // C5
                oscillator.frequency.setValueAtTime(659.25, now + 0.1); // E5
                oscillator.frequency.setValueAtTime(783.99, now + 0.2); // G5
                oscillator.frequency.setValueAtTime(1046.50, now + 0.3); // C6
                gainNode.gain.setValueAtTime(0.2, now);
                gainNode.gain.linearRampToValueAtTime(0.2, now + 0.4);
                gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.5);
                oscillator.start(now);
                oscillator.stop(now + 0.5);
                break;
            case 'lose':
                oscillator.type = 'sawtooth';
                oscillator.frequency.setValueAtTime(300, now);
                oscillator.frequency.linearRampToValueAtTime(100, now + 0.5);
                gainNode.gain.setValueAtTime(0.1, now);
                gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.5);
                oscillator.start(now);
                oscillator.stop(now + 0.5);
                break;
            case 'draw':
                oscillator.type = 'square';
                oscillator.frequency.setValueAtTime(200, now);
                oscillator.frequency.setValueAtTime(200, now + 0.2);
                gainNode.gain.setValueAtTime(0.05, now);
                gainNode.gain.setValueAtTime(0.05, now + 0.2);
                gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.3);
                oscillator.start(now);
                oscillator.stop(now + 0.3);
                break;
        }
    }

    spawnConfetti() {
        const colors = ['#ff5757', '#5ce1e6', '#7ed957', '#FFDE59', '#FF9F43'];
        for (let i = 0; i < 50; i++) {
            const confetti = document.createElement('div');
            confetti.style.position = 'fixed';
            confetti.style.width = '10px';
            confetti.style.height = '10px';
            confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
            confetti.style.left = Math.random() * 100 + 'vw';
            confetti.style.top = '-10px';
            confetti.style.zIndex = '9999';
            confetti.style.borderRadius = '2px';
            document.body.appendChild(confetti);

            const animation = confetti.animate([
                { transform: `translate3d(0, 0, 0) rotate(0deg)`, opacity: 1 },
                { transform: `translate3d(${(Math.random() - 0.5) * 200}px, 100vh, 0) rotate(${Math.random() * 360}deg)`, opacity: 0 }
            ], {
                duration: 2000 + Math.random() * 1000,
                easing: 'cubic-bezier(0, .9, .57, 1)'
            });

            animation.onfinish = () => confetti.remove();
        }
    }

    screenShake() {
        const body = document.body;
        const animation = body.animate([
            { transform: 'translate(1px, 1px) rotate(0deg)' },
            { transform: 'translate(-1px, -2px) rotate(-1deg)' },
            { transform: 'translate(-3px, 0px) rotate(1deg)' },
            { transform: 'translate(3px, 2px) rotate(0deg)' },
            { transform: 'translate(1px, -1px) rotate(1deg)' },
            { transform: 'translate(-1px, 2px) rotate(-1deg)' },
            { transform: 'translate(-3px, 1px) rotate(0deg)' },
            { transform: 'translate(3px, 1px) rotate(-1deg)' },
            { transform: 'translate(-1px, -1px) rotate(1deg)' },
            { transform: 'translate(1px, 2px) rotate(0deg)' },
            { transform: 'translate(1px, -2px) rotate(-1deg)' }
        ], {
            duration: 500,
            iterations: 1
        });
    }

    _getQueryParam(name) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    }

    /**
     * Reports score to the main application
     * @param {number} score
     */
    reportScore(score) {
        console.log(`[GameEngine] ${this.id} reported score: ${score}`);
        const currentPts = parseInt(localStorage.getItem('kz-pts') || '0');
        localStorage.setItem('kz-pts', currentPts + score);
        
        // High score logic
        const highScore = this.getHighScore();
        if (score > highScore) {
            localStorage.setItem(`kz-hs-${this.id}`, score);
        }

        localStorage.setItem(`kz-prog-${this.id}`, '100'); 

        if (this.onScoreUpdate) {
            this.onScoreUpdate(score);
        }
    }

    /**
     * Returns high score for current game
     */
    getHighScore() {
        return parseInt(localStorage.getItem(`kz-hs-${this.id}`) || '0');
    }

    /**
     * Vibration feedback
     * @param {number} ms 
     */
    vibrate(ms = 50) {
        if (this.isMuted) return;
        if (navigator.vibrate) {
            navigator.vibrate(ms);
        }
    }

    /**
     * Comprehensive Game Over handling
     * @param {number} finalScore 
     */
    gameOver(finalScore) {
        this.reportScore(finalScore);
        this.adCounter++;
        localStorage.setItem('kz-ad-cnt', this.adCounter);

        // Show ad every 3rd game over to maintain UX
        if (this.adCounter % 3 === 0) {
            this.showAd();
        }
    }

    /**
     * Shows interstitial ad via Android Bridge
     */
    showAd() {
        try {
            // Check standard WebView interface
            if (window.AndroidAdMob && window.AndroidAdMob.showInterstitial) {
                window.AndroidAdMob.showInterstitial();
            } 
            // Fallback for iframe setups
            else if (window.parent && window.parent.AndroidAdMob) {
                window.parent.AndroidAdMob.showInterstitial();
            } else {
                console.warn("[GameEngine] Android Bridge not found");
            }
        } catch (e) {
            console.error("[GameEngine] Ad show error:", e);
        }
    }

    /**
     * To be overridden by games
     * @param {boolean} muted
     */
    onMuteChange(muted) {
        // Implementation in specific games
    }

    /**
     * Localization helper
     * @param {Object} translations
     * @returns {Object}
     */
    getTranslations(translations) {
        return translations[this.lang] || translations['en'];
    }
}

// Export for usage in games
window.KidZoneGame = KidZoneGame;
