package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.Product;
import com.programacao_web.rpg_market.model.ProductCategory;
import com.programacao_web.rpg_market.model.ItemRarity;
import com.programacao_web.rpg_market.model.ProductStatus;
import com.programacao_web.rpg_market.model.ProductType;
import com.programacao_web.rpg_market.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/mercado")
public class MarketController {

    @Autowired
    private ProductService productService;
    
    // Página principal do mercado
    @GetMapping
    public String showMarketplace(
            Model model, 
            @Qualifier("products") @PageableDefault(size = 6, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable productPageable,
            
            @Qualifier("auctions") @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable auctionPageable) {
        
        // Produtos de venda direta disponíveis
        model.addAttribute("products", productService.findAvailable(productPageable).getContent());
        
        // Leilões ativos
        model.addAttribute("auctions", productService.findActiveAuctions(auctionPageable).getContent());
        
        // Categorias para navegação
        model.addAttribute("categories", ProductCategory.values());
        
        return "market/index";
    }
    
    // Exibe produtos de uma categoria específica
    @GetMapping("/categoria/{category}")
    public String showCategoryProducts(
            @PathVariable ProductCategory category,
            Model model,
            @PageableDefault(size = 12) Pageable pageable) {
        
        model.addAttribute("products", productService.findByCategory(category, pageable));
        model.addAttribute("currentCategory", category);
        model.addAttribute("categories", ProductCategory.values());
        return "market/category";
    }
    
    // Busca produtos por nome
    @GetMapping("/buscar")
    public String searchProducts(
            @RequestParam(required = false) String keyword,
            Model model,
            @PageableDefault(size = 12) Pageable pageable) {
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("products", 
                    productService.search(keyword, ProductStatus.AVAILABLE, pageable));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("products", productService.findAvailable(pageable));
        }
        
        model.addAttribute("categories", ProductCategory.values());
        return "market/search";
    }
    
    // Exibe todos os leilões ativos com opções de filtro e ordenação
    @GetMapping("/masmorra-dos-leiloes")
    public String showAuctions(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) ItemRarity rarity,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean endingSoon,
            @RequestParam(required = false, defaultValue = "auctionEndDate,asc") String sort,
            Model model,
            @PageableDefault(size = 12) Pageable pageable) {
        
        // Processar parâmetros de ordenação
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        // Criar pageable com ordenação específica
        PageRequest pageRequest = PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            Sort.by(direction, sortField)
        );
          // Usar o novo método de filtro
        model.addAttribute("auctions", productService.findAuctionsWithFilters(
            category, rarity, minPrice, maxPrice, endingSoon, pageRequest));
        
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("rarities", ItemRarity.values());
        
        return "market/auctions";
    }
    
    // Exibe ranking de vendedores
    @GetMapping("/ranking-dos-nobres")
    public String showRanking(Model model) {
        model.addAttribute("topSellers", productService.getTopSellers());
        model.addAttribute("topBuyers", productService.getTopBuyers());
        return "market/ranking";
    }
      /**
     * Exibe todos os itens de venda direta com opções de filtro e ordenação
     */
    @GetMapping("/vendas-diretas")
    public String showDirectSales(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) ItemRarity rarity,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        // Filtrar produtos de venda direta (não leilões)
        Page<Product> productsPage = productService.findDirectSalesWithFilters(
            category, 
            rarity,
            minPrice, 
            maxPrice, 
            pageable
        );
          model.addAttribute("products", productsPage);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("rarities", ItemRarity.values());
        
        return "market/direct-sales";
    }
}
