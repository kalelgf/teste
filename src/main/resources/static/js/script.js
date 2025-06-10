/**
 * Script personalizado para o RPG Market
 */

document.addEventListener('DOMContentLoaded', function() {
    // Inicializa tooltips do Bootstrap
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Inicializa popovers do Bootstrap
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.forEach(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
    
    // Funções específicas para determinadas páginas
    if (document.getElementById('countdown')) {
        updateAuctionCountdown();
    }
      // Initialize all countdowns in inventory
    const inventoryCountdowns = document.querySelectorAll('.countdown');
    
    if (inventoryCountdowns.length > 0) {
        updateAllCountdowns();
        // Update countdowns every second instead of every minute
        setInterval(updateAllCountdowns, 1000);
    }

    // For form submission - prevent double submission
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function() { // Removed unused 'e' parameter
            // Disable submit button on click to prevent double submission
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
            }
        });
    });

    // Password change form validation
    const passwordForm = document.querySelector('form[action="/aventureiro/senha"]');
    if (passwordForm) {
        const newPassword = document.getElementById('newPassword');
        const confirmPassword = document.getElementById('confirmPassword');
        
        passwordForm.addEventListener('submit', function(e) {
            let isValid = true;
            
            // Check password length
            if (newPassword.value.length < 6) {
                showValidationError(newPassword, 'A senha deve ter pelo menos 6 caracteres');
                isValid = false;
            } else {
                clearValidationError(newPassword);
            }
            
            // Check password match
            if (newPassword.value !== confirmPassword.value) {
                showValidationError(confirmPassword, 'As senhas não coincidem');
                isValid = false;
            } else {
                clearValidationError(confirmPassword);
            }
            
            if (!isValid) {
                e.preventDefault(); // Prevent form submission
            }
        });
    }

    // Mostrar/esconder campos de leilão com animação
    const typeSelect = document.getElementById('type');
    const directSaleFields = document.getElementById('directSaleFields');
    const auctionFields = document.getElementById('auctionFields');

    if (typeSelect && directSaleFields && auctionFields) {
        // Inicializar os campos corretamente no carregamento da página
        if (typeSelect.value === 'AUCTION') {
            directSaleFields.style.display = 'none';
            auctionFields.style.display = 'block';
            setTimeout(() => {
                auctionFields.classList.add('show');
            }, 10);
        } else {
            auctionFields.style.display = 'none';
            directSaleFields.style.display = 'block';
        }
        
        typeSelect.addEventListener('change', function() {
            if (this.value === 'AUCTION') {
                // Esconder campos de venda direta
                directSaleFields.classList.remove('show');
                setTimeout(() => {
                    directSaleFields.style.display = 'none';
                    // Mostrar campos de leilão
                    auctionFields.style.display = 'block';
                    setTimeout(() => {
                        auctionFields.classList.add('show');
                        // Definir o campo de lance inicial como obrigatório
                        document.getElementById('startingBid').required = true;
                        document.getElementById('directSalePrice').required = false;
                    }, 10);
                }, 300);
            } else {
                // Esconder campos de leilão
                auctionFields.classList.remove('show');
                setTimeout(() => {
                    auctionFields.style.display = 'none';
                    // Mostrar campos de venda direta
                    directSaleFields.style.display = 'block';
                    setTimeout(() => {
                        directSaleFields.classList.add('show');
                        // Definir o campo de preço como obrigatório
                        document.getElementById('startingBid').required = false;
                        document.getElementById('directSalePrice').required = true;
                    }, 10);
                }, 300);
            }
        });
    }

    // Add event listeners for form fields if they exist
    const nameInput = document.getElementById('name');
    const descInput = document.getElementById('description');
    const priceInput = document.getElementById('price');
    const categorySelect = document.getElementById('category');
    const imageInput = document.getElementById('image');

    if (nameInput) nameInput.addEventListener('input', updatePreview);
    if (descInput) descInput.addEventListener('input', updatePreview);
    if (priceInput) priceInput.addEventListener('input', updatePreview);
    if (categorySelect) categorySelect.addEventListener('change', updatePreview);

    if (imageInput) {
        imageInput.addEventListener('change', function() {
            const previewImage = document.getElementById('previewImage');
            if (this.files && this.files[0] && previewImage) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    previewImage.src = e.target.result;
                };
                reader.readAsDataURL(this.files[0]);
            }
        });
    }

    // NOVA IMPLEMENTAÇÃO PARA O DROPDOWN DE USUÁRIO
    const userDropdown = document.getElementById('userDropdown');
    const userDropdownMenu = userDropdown ? userDropdown.nextElementSibling : null;
    
    if (userDropdown && userDropdownMenu && userDropdownMenu.classList.contains('dropdown-menu')) {
        // 1. Desativar qualquer comportamento padrão do Bootstrap
        userDropdown.setAttribute('data-bs-toggle', '');
        userDropdown.setAttribute('data-bs-auto', 'false');
        
        // 2. Adicionar manipulador de clique simplificado
        userDropdown.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            // Toggle simples da classe show
            if (userDropdownMenu.classList.contains('show')) {            userDropdownMenu.classList.remove('show');
            userDropdown.setAttribute('aria-expanded', 'false');
            } else {            userDropdownMenu.classList.add('show');
            userDropdown.setAttribute('aria-expanded', 'true');
            }
        });
        
        // 3. Fechar ao clicar fora (document)
        document.addEventListener('click', function(e) {
            if (userDropdownMenu.classList.contains('show') && 
                !userDropdown.contains(e.target) && 
                !userDropdownMenu.contains(e.target)) {
                userDropdownMenu.classList.remove('show');
                userDropdown.setAttribute('aria-expanded', 'false');
            }
        });
    }
});

/**
 * Atualiza o contador regressivo para o término do leilão
 */
function updateAuctionCountdown() {
    const endDateElement = document.getElementById('endDate');
    if (!endDateElement) return;
    
    const endDate = new Date(endDateElement.value);
    const countdownElement = document.getElementById('countdown');
    
    function update() {
        const now = new Date();
        const diff = endDate - now;
        
        if (diff <= 0) {
            countdownElement.textContent = "Leilão Encerrado";
            return;
        }
        
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        
        let displayText = "";
        if(days > 0) displayText += days + "d ";
        displayText += hours + "h " + minutes + "m " + seconds + "s";
        
        countdownElement.textContent = displayText;
    }
    
    // Atualizar agora e a cada segundo
    update();
    setInterval(update, 1000);
}

/**
 * Updates all countdowns on inventory pages
 */
function updateAllCountdowns() {
    const countdowns = document.querySelectorAll('.countdown');
    
    countdowns.forEach(element => {
        try {
            const dateStr = element.getAttribute('data-end-date');
            if (!dateStr) {
                console.error('Missing data-end-date attribute on countdown element');
                return;
            }

            const endDate = new Date(dateStr);
            if (isNaN(endDate)) {
                console.error('Invalid date format in data-end-date:', dateStr);
                return;
            }
            
            const now = new Date();
            const diff = endDate - now;
            
            if (diff <= 0) {
                element.textContent = "Encerrado";
                element.classList.add('text-danger');
                return;
            }
            
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((diff % (1000 * 60)) / 1000);
            
            let displayText = "";
            if(days > 0) displayText += days + "d ";
            displayText += hours + "h " + minutes + "m " + seconds + "s";
            
            element.textContent = displayText;
        } catch (e) {
            console.error('Error updating countdown:', e);
        }
    });
}

/**
 * Show validation error for form inputs
 */
function showValidationError(inputElement, message) {
    // Clear any existing error
    clearValidationError(inputElement);
    
    // Add is-invalid class to the input
    inputElement.classList.add('is-invalid');
    
    // Create and append error message
    const errorDiv = document.createElement('div');
    errorDiv.className = 'invalid-feedback';
    errorDiv.textContent = message;
    inputElement.parentNode.appendChild(errorDiv);
}

/**
 * Clear validation error for form inputs
 */
function clearValidationError(inputElement) {
    inputElement.classList.remove('is-invalid');
    const existingError = inputElement.parentNode.querySelector('.invalid-feedback');
    if (existingError) {
        existingError.remove();
    }
}

/**
 * Live preview update
 */
function updatePreview() {
    // Name
    const previewName = document.getElementById('previewName');
    const nameInput = document.getElementById('name');
    if (previewName && nameInput) {
        previewName.textContent = nameInput.value || 'Nome do Item';
    }
    
    // Description
    const previewDesc = document.getElementById('previewDescription');
    const descInput = document.getElementById('description');
    if (previewDesc && descInput) {
        previewDesc.textContent = descInput.value || 'Descrição do item...';
    }
    
    // Price
    const previewPrice = document.getElementById('previewPrice');
    const priceInput = document.getElementById('price');
    if (previewPrice && priceInput) {
        const price = priceInput.value || 0;
        previewPrice.textContent = '$' + parseFloat(price).toFixed(2);
    }
    
    // Category
    const previewCategory = document.getElementById('previewCategory');
    const categorySelect = document.getElementById('category');
    if (previewCategory && categorySelect && categorySelect.selectedIndex > 0) {
        previewCategory.textContent = categorySelect.options[categorySelect.selectedIndex].text;
    }
    
    // Rarity (if exists)
    const raritySelect = document.getElementById('rarity');
    const previewCard = document.querySelector('.preview-card');
    
    if (raritySelect && previewCard && raritySelect.selectedIndex > 0) {
        const rarityValue = raritySelect.value.toLowerCase();
        
        // Remove previous rarity classes
        previewCard.classList.remove('border-common', 'border-uncommon', 'border-rare', 'border-epic', 'border-legendary');
        
        // Add appropriate class based on selection
        if (rarityValue) {
            previewCard.classList.add('border-' + rarityValue);
        }
    }
}