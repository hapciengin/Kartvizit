document.addEventListener('DOMContentLoaded', function() {
    const editButton = document.getElementById('edit-mode-btn');

    // GridStack'i başlat
    const grid = GridStack.init({
        // Başlangıçta statik (sürüklenemez/boyutlandırılamaz)
        staticGrid: true,
        // Diğer GridStack ayarları
        float: true,
        cellHeight: '70px', // Örnek bir yükseklik, ihtiyaca göre değiştirilebilir
        margin: 10,
        column: 12
    });

    if (!editButton) {
        return;
    }

    let isEditMode = false;

    // Düzenleme modunu aç/kapat
    function toggleEditMode() {
        isEditMode = !isEditMode;
        grid.static(isEditMode, true); // Grid'i düzenlenebilir veya statik yap

        if (isEditMode) {
            editButton.textContent = 'Kaydet ve Çık';
            editButton.classList.remove('saving');
            document.body.classList.add('edit-mode'); // CSS için
        } else {
            editButton.textContent = 'Kaydediliyor...';
            editButton.classList.add('saving');
            document.body.classList.remove('edit-mode');

            // Değişiklikleri kaydet
            saveLayout();

            setTimeout(() => {
                editButton.textContent = 'Yerleşimi Düzenle';
                editButton.classList.remove('saving');
                grid.static(true, true); // Kaydettikten sonra tekrar statik yap
            }, 1000);
        }
    }

    // Değişiklikleri Android'e gönder
    function saveLayout() {
        // GridStack'in save metodu güncel pozisyonları döner
        const serializedData = grid.save(false);

        // Android arayüzüne göndermek için veriyi yeniden formatla
        const layoutData = serializedData.map(item => ({
            id: item.id, // gs-id
            gridX: item.x,
            gridY: item.y,
            gridW: item.w,
            gridH: item.h
        }));

        if (typeof Android !== 'undefined' && Android.saveLayout) {
            Android.saveLayout(JSON.stringify(layoutData));
        } else {
            console.log(JSON.stringify(layoutData));
        }
    }

    // Butonun tıklama olayını ayarla
    editButton.addEventListener('click', toggleEditMode);
});