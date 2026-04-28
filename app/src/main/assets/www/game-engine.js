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
        localStorage.setItem(`kz-prog-${this.id}`, '100'); // Mark as completed/played

        if (this.onScoreUpdate) {
            this.onScoreUpdate(score);
        }
    }

    /**
     * Shows interstitial ad via Android Bridge
     */
    showAd() {
        if (window.parent && window.parent.AndroidAdMob) {
            window.parent.AndroidAdMob.showInterstitial();
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
