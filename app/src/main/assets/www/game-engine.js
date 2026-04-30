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
