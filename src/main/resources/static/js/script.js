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
    
    // Inicializar dropdowns do usuário
    initUserDropdowns();
      // Funções específicas para determinadas páginas
    if (document.getElementById('countdown')) {
        updateAuctionCountdown();
    }
    
    // Inicializar dropdowns do usuário
    initUserDropdowns();
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
                        auctionFields.classList.add('show');                        // Definir o campo de lance inicial como obrigatório
                        document.getElementById('startingBid').required = true;
                        document.getElementById('directSalePrice').required = false;
                        
                        // Atualizar a prévia quando mudar para leilão
                        if (typeof updatePreviewFromFields === 'function') {
                            updatePreviewFromFields();
                        }
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
                        
                        // Atualizar a prévia quando mudar para venda direta
                        if (typeof updatePreviewFromFields === 'function') {
                            updatePreviewFromFields();
                        }
                    }, 10);
                }, 300);
            }
        });
    }    // Add event listeners for form fields if they exist - REMOVIDO para evitar conflito com o updatePreviewFromFields

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
    }    // Executar as novas funções quando o DOM estiver pronto
    setupImageFallback();
    setupMobileScrolling();
    optimizeMobilePerformance();
    initUserDropdowns();
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
// Função de preview simplificada - apenas para compatibilidade
function updatePreview() {
    // Esta função foi substituída pela updatePreviewFromFields no template create.html
    // Mantida apenas para compatibilidade com código existente
    
    // Se existir a função mais avançada, usar ela
    if (typeof updatePreviewFromFields === 'function') {
        updatePreviewFromFields();
        return;
    }
    
    // Fallback básico apenas
    const previewName = document.getElementById('previewName');
    const nameInput = document.getElementById('name');
    if (previewName && nameInput) {
        previewName.textContent = nameInput.value || 'Nome do Item';
    }
}

// Função para melhorar fallback de imagens
function setupImageFallback() {
    // Setup fallback para avatares de usuário
    const userAvatars = document.querySelectorAll('.user-avatar');
    userAvatars.forEach(avatar => {
        avatar.addEventListener('error', function() {
            this.style.backgroundImage = "url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAiIGhlaWdodD0iMzAiIHZpZXdCb3g9IjAgMCAzMCAzMCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTUiIGN5PSIxNSIgcj0iMTUiIGZpbGw9IiNkNGFmMzciLz4KPHBhdGggZD0iTTE1IDhDMTIuNzkgOCAxMSA9Ljc5IDExIDEyQzExIDE0LjIxIDEyLjc5IDE2IDE1IDE2QzE3LjIxIDE2IDE5IDE0LjIxIDE5IDEyQzE5IDkuNzkgMTcuMjEgOCAxNSA4WiIgZmlsbD0iIzJjMmMyYyIvPgo8cGF0aCBkPSJNMTUgMThDMTEuNjkgMTggOSAyMC42OSA5IDI0VjI2SDE1SDIxVjI0QzIxIDIwLjY5IDE4LjMxIDE4IDE1IDE4WiIgZmlsbD0iIzJjMmMyYyIvPgo8L3N2Zz4K')";
            this.style.backgroundSize = 'cover';
            this.style.backgroundPosition = 'center';
            this.src = '';
        });
    });
    
    // Setup fallback para imagens de produtos
    const productImages = document.querySelectorAll('.product-img');
    productImages.forEach(img => {
        img.addEventListener('error', function() {
            this.style.backgroundImage = "url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIiBmaWxsPSIjZjhmOWZhIi8+CjxwYXRoIGQ9Ik0xMDAgNjBDODYuNzQ3IDYwIDc2IDcwLjc0NyA3NiA4NEg3NkM3NiA4NC44IDc2LjggODQuOCA3Ni44IDg0SDc2VjEwNkg3NkM3NiAxMTQuNCA4NC42IDEyNCAxMDAgMTI0QzExNS40IDEyNCAxMjQgMTE0LjQgMTI0IDEwNlYxMDZIMTI0VjEwNkgxMjRWODRDMTI0IDcwLjc0NyAxMTMuMjUzIDYwIDEwMCA2MFoiIGZpbGw9IiNkNGFmMzciLz4KPHBhdGggZD0iTTEwMCAxNDBIMTAwQzEwMy4zMTQgMTQwIDEwNiAxNDIuNjg2IDEwNiAxNDZIMTA2VjE0Nkg5NFYxNDZDOTQgMTQyLjY4NiA5Ni42ODYgMTQwIDEwMCAxNDBaIiBmaWxsPSIjZDRhZjM3Ii8+Cjx0ZXh0IHg9IjEwMCIgeT0iMTcwIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiM2NjY2NjYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPkl0ZW0gTcOhZ2ljbzwvdGV4dD4KPC9zdmc+')";
            this.style.backgroundSize = 'cover';
            this.style.backgroundPosition = 'center';
            this.src = '';
        });
    });
}

// Função para smooth scroll em dispositivos móveis
function setupMobileScrolling() {
    // Adiciona scroll suave para navegação
    const navLinks = document.querySelectorAll('.nav-link:not(.dropdown-toggle)');
    navLinks.forEach(link => {
        link.addEventListener('click', function() {
            // Fechar menu mobile se estiver aberto após clique em link
            const navbarCollapse = document.getElementById('navbarMain');
            if (navbarCollapse && navbarCollapse.classList.contains('show')) {
                const collapseInstance = bootstrap.Collapse.getInstance(navbarCollapse);
                if (collapseInstance) {
                    collapseInstance.hide();
                }
            }
        });
    });
}

// Função para reinicializar dropdowns quando a tela redimensiona
function handleWindowResize() {
    // Fechar todos os dropdowns quando redimensionar
    const openDropdowns = document.querySelectorAll('.dropdown-menu.show');
    openDropdowns.forEach(dropdown => {
        dropdown.classList.remove('show');
    });
    
    // Reinicializar dropdowns do usuário
    initUserDropdowns();
}

// Função para otimizar performance em mobile
function optimizeMobilePerformance() {
    // Lazy loading para imagens se disponível
    if ('IntersectionObserver' in window) {
        const images = document.querySelectorAll('img[data-src]');
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.removeAttribute('data-src');
                    imageObserver.unobserve(img);
                }
            });
        });
        
        images.forEach(img => imageObserver.observe(img));
    }
    
    // Otimizar animações em devices com movimento reduzido
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        const style = document.createElement('style');
        style.textContent = `
            *, *::before, *::after {
                animation-duration: 0.01ms !important;
                animation-iteration-count: 1 !important;
                transition-duration: 0.01ms !important;
            }
        `;
        document.head.appendChild(style);
    }
}

// Função para corrigir dropdown em mobile
function fixMobileDropdown() {
    // Garantir que os dropdowns do Bootstrap funcionem corretamente
    const dropdowns = document.querySelectorAll('[data-bs-toggle="dropdown"]');
    
    dropdowns.forEach(dropdown => {
        // Remover listeners antigos
        dropdown.removeEventListener('click', handleDropdownToggle);
        dropdown.addEventListener('click', handleDropdownToggle);
    });
    
    function handleDropdownToggle(e) {
        // Em mobile, permitir que o Bootstrap gerencie o dropdown normalmente
        if (window.innerWidth <= 991) {
            // Não prevenir o comportamento padrão, deixar o Bootstrap funcionar
            return true;
        }
    }
}

// Função para melhorar navegação mobile  
function enhanceMobileNavigation() {
    const navbarToggler = document.querySelector('.navbar-toggler');
    const navbarCollapse = document.querySelector('.navbar-collapse');
    
    if (navbarToggler && navbarCollapse) {
        // Quando o menu mobile fechar, garantir que dropdowns também fechem
        navbarCollapse.addEventListener('hidden.bs.collapse', function() {
            // Fechar todos os dropdowns abertos
            const openDropdowns = document.querySelectorAll('.dropdown-menu.show');
            openDropdowns.forEach(menu => {
                menu.classList.remove('show');
            });
            
            // Resetar aria-expanded dos toggles
            const dropdownToggles = document.querySelectorAll('[data-bs-toggle="dropdown"]');
            dropdownToggles.forEach(toggle => {
                toggle.setAttribute('aria-expanded', 'false');
            });
        });
    }
    
    // Melhorar comportamento de clique nos links do dropdown
    const dropdownItems = document.querySelectorAll('.dropdown-item');
    dropdownItems.forEach(item => {
        item.addEventListener('click', function(e) {
            // Se for um botão de form, não interferir
            if (this.tagName === 'BUTTON') {
                return true;
            }
            
            // Para links normais, permitir navegação
            if (this.href && this.href !== '#') {
                return true;
            }
        });
    });
}

// Debug para dropdown mobile
function debugMobileDropdown() {
    const userDropdown = document.getElementById('userDropdown');
    const dropdownMenu = document.querySelector('#userDropdown + .dropdown-menu');
    
    if (userDropdown) {
        // Adicionar listener de debug
        userDropdown.addEventListener('click', function(e) {
            // Pode adicionar lógica de debug aqui se necessário
        });
    }
}

// ===== DROPDOWN DO USUÁRIO MOBILE/DESKTOP =====
function initUserDropdowns() {
    
    // Aguardar o Bootstrap estar disponível
    if (typeof bootstrap === 'undefined') {
        setTimeout(initUserDropdowns, 100);
        return;
    }
    
    const userDropdownMobile = document.getElementById('userDropdownMobile');
    const userDropdownDesktop = document.getElementById('userDropdownDesktop');
    const navbarToggler = document.querySelector('.navbar-toggler');
    const navbarCollapse = document.getElementById('navbarMain');
    
    // Configurar dropdown mobile
    if (userDropdownMobile) {
        
        let dropdownInstance = bootstrap.Dropdown.getInstance(userDropdownMobile);
        if (!dropdownInstance) {
            dropdownInstance = new bootstrap.Dropdown(userDropdownMobile, {
                autoClose: 'outside',
                boundary: 'viewport'
            });
        }
        
        // Event listeners para mobile
        userDropdownMobile.addEventListener('click', function(e) {
            e.stopPropagation();
        });
        
        userDropdownMobile.addEventListener('show.bs.dropdown', function() {
            // Fechar menu principal se estiver aberto
            if (navbarCollapse && navbarCollapse.classList.contains('show')) {
                const collapseInstance = bootstrap.Collapse.getInstance(navbarCollapse);
                if (collapseInstance) {
                    collapseInstance.hide();
                }
            }
        });
        
        userDropdownMobile.addEventListener('shown.bs.dropdown', function() {
            // Dropdown mobile aberto com sucesso
        });
        
        userDropdownMobile.addEventListener('hide.bs.dropdown', function() {
            // Dropdown mobile sendo fechado
        });
    }    // Configurar dropdown desktop
    if (userDropdownDesktop) {
        
        let dropdownInstance = bootstrap.Dropdown.getInstance(userDropdownDesktop);
        if (!dropdownInstance) {
            dropdownInstance = new bootstrap.Dropdown(userDropdownDesktop, {
                autoClose: true,
                boundary: 'viewport'
            });
        }
    }
    
    // Gerenciar conflito entre menu principal e dropdown do usuário
    if (navbarToggler && navbarCollapse) {
        navbarCollapse.addEventListener('show.bs.collapse', function() {
            // Fechar dropdown do usuário se estiver aberto quando menu principal abrir
            if (userDropdownMobile) {
                const dropdownInstance = bootstrap.Dropdown.getInstance(userDropdownMobile);
                if (dropdownInstance) {
                    dropdownInstance.hide();
                }
            }
        });
        
        navbarCollapse.addEventListener('hide.bs.collapse', function() {
            // Menu principal fechando
        });
    }
}

// ===== INICIALIZAÇÃO =====
document.addEventListener('DOMContentLoaded', function() {
    // Funções básicas
    setupImageFallback();
    setupMobileScrolling();
    optimizeMobilePerformance();
      // Aguardar o Bootstrap carregar completamente
    setTimeout(() => {
        // Inicializar dropdowns do usuário
        if (typeof bootstrap !== 'undefined') {
            initUserDropdowns();
        }
    }, 100);
});

// Adicionar listener para redimensionamento da janela
window.addEventListener('resize', handleWindowResize);

// Reinicializar quando a orientação mudar (mobile)
window.addEventListener('orientationchange', function() {
    setTimeout(function() {
        handleWindowResize();
        initUserDropdowns();
    }, 100);
});

// Reinicializar dropdowns quando redimensionar
window.addEventListener('resize', function() {
    setTimeout(initUserDropdowns, 100);
});