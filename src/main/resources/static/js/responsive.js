/**
 * Script para suporte a funcionalidades responsivas do RPG Market
 */
document.addEventListener('DOMContentLoaded', function() {
    // Implementação do toggle para filtros em mobile
    const filterToggleBtn = document.querySelector('.filter-toggle-btn');
    const filterSidebar = document.querySelector('.filter-sidebar');
    
    if (filterToggleBtn && filterSidebar) {
        filterToggleBtn.addEventListener('click', function() {
            filterSidebar.classList.toggle('collapsed');
            
            // Altera o texto do botão
            if (filterSidebar.classList.contains('collapsed')) {
                filterToggleBtn.textContent = 'Mostrar Filtros';
            } else {
                filterToggleBtn.textContent = 'Ocultar Filtros';
            }
        });
        
        // Inicializa como colapsado em mobile
        if (window.innerWidth < 992) {
            filterSidebar.classList.add('collapsed');
        }
    }
    
    // Ajustar espaçamento do body quando o navbar fixo é usado
    function adjustBodyPadding() {
        if (window.innerWidth <= 768) {
            const navbarHeight = document.querySelector('.navbar')?.offsetHeight || 0;
            document.body.style.paddingTop = navbarHeight + 'px';
        } else {
            document.body.style.paddingTop = '0';
        }
    }
    
    // Executar no carregamento e quando a janela for redimensionada
    adjustBodyPadding();
    window.addEventListener('resize', adjustBodyPadding);
    
    // Layout responsivo para tabelas
    const tables = document.querySelectorAll('table.table');
    tables.forEach(table => {
        const wrapper = document.createElement('div');
        wrapper.className = 'table-responsive';
        table.parentNode.insertBefore(wrapper, table);
        wrapper.appendChild(table);
    });
    
    // Adicionar classe necessária para responsividade em botões específicos
    if (window.innerWidth <= 576) {
        const actionButtons = document.querySelectorAll('.action-btn');
        actionButtons.forEach(btn => {
            btn.classList.add('btn-block-mobile');
        });
    }
    
    // Ajustes para cards em grid
    function adjustGridLayout() {
        const productCards = document.querySelectorAll('.product-card');
        
        if (window.innerWidth <= 576) {
            // Vamos garantir que os cards tenham mesma altura em uma linha
            let rowCards = [];
            let prevOffsetTop = -1;
            
            productCards.forEach(card => {
                const currentOffsetTop = card.offsetTop;
                
                if (prevOffsetTop !== -1 && currentOffsetTop !== prevOffsetTop) {
                    // Nova linha detectada, ajustar altura da linha anterior
                    const maxHeight = Math.max(...rowCards.map(c => c.offsetHeight));
                    rowCards.forEach(c => c.style.height = maxHeight + 'px');
                    rowCards = [];
                }
                
                rowCards.push(card);
                prevOffsetTop = currentOffsetTop;
            });
            
            // Processar a última linha
            if (rowCards.length > 0) {
                const maxHeight = Math.max(...rowCards.map(c => c.offsetHeight));
                rowCards.forEach(c => c.style.height = maxHeight + 'px');
            }
        } else {
            // Resetar alturas para desktop
            productCards.forEach(card => {
                card.style.height = '';
            });
        }
    }
    
    // Transformar tabelas comuns em responsivas (estilo stack em mobile)
    function makeTablesResponsive() {
        const tables = document.querySelectorAll('.table-responsive-stack');
        
        tables.forEach(table => {
            const headerCells = table.querySelectorAll('thead th');
            const headerTexts = Array.from(headerCells).map(cell => cell.textContent.trim());
            
            const bodyRows = table.querySelectorAll('tbody tr');
            
            bodyRows.forEach(row => {
                const cells = row.querySelectorAll('td');
                
                cells.forEach((cell, index) => {
                    if (index < headerTexts.length) {
                        cell.setAttribute('data-label', headerTexts[index]);
                    }
                });
            });
        });
    }
    
    // Corrigir altura de imagens em containers responsivos
    function fixImageContainers() {
        const imgContainers = document.querySelectorAll('.img-container-square');
        
        imgContainers.forEach(container => {
            const width = container.offsetWidth;
            container.style.height = width + 'px';
        });
    }
    
    // Executar funções responsivas
    makeTablesResponsive();
    window.addEventListener('resize', fixImageContainers);
    window.addEventListener('load', fixImageContainers);
    
    // Adicionar classes responsivas dinamicamente
    if (window.innerWidth <= 576) {
        document.querySelectorAll('.make-btn-block-xs').forEach(btn => {
            btn.classList.add('btn-block');
            btn.classList.add('w-100');
        });
    }
    
    // Executar após carregamento completo e em resize
    window.addEventListener('load', adjustGridLayout);
    window.addEventListener('resize', adjustGridLayout);
});
