// Portal animation
function initPortal() {
    const entryScreen = document.getElementById('entry-screen');
    const portal = document.getElementById('portal-overlay');
    const panel = document.querySelector('.panel-layout');
    const sound = document.getElementById('portal-sound');
    const soundLoaded = document.getElementById('portal-sound-loaded');
    sound.volume = 0.3;
    soundLoaded.volume = 0.3;

    sound.play();

    portal.style.display = 'block';
    entryScreen.style.opacity = '0';
    setTimeout(() => {
        entryScreen.style.display = 'none';
        panel.style.display = 'block';
        setTimeout(() => {
            portal.style.transition = 'opacity 1s ease';
            portal.style.opacity = '0';
            setTimeout(() => {
                portal.style.display = 'none';
            }, 500);
            soundLoaded.play();
        }, 3500);

    }, 800);
}