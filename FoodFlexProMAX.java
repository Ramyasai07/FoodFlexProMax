import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Random;
import java.io.*;
import javax.swing.Timer;

public class FoodFlexProMAX {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FoodFlexGUI app = new FoodFlexGUI();
            app.setVisible(true);
        });
    }
}

// ================== ENHANCED DARK THEME UI ==================
class FoodFlexGUI extends JFrame implements OrderProcessor.OrderUpdateListener {
    private final MenuManager menuManager = new MenuManager();
    private final Cart cart = new Cart();
    private Restaurant currentRestaurant;
    
    // UI Components
    private JTabbedPane menuTabs;
    private JList<Meal> cartList;
    private DefaultListModel<Meal> cartModel = new DefaultListModel<>();
    private JProgressBar progressBar;
    private JLabel totalLabel;
    private JLabel restaurantLabel;
    private JLabel calorieLabel;
    
    // Colors
    private final Color DARK_BG = new Color(40, 40, 45);
    private final Color CARD_BG = new Color(60, 60, 65);
    private final Color ACCENT = new Color(255, 105, 50);
    private final Color TEXT_WHITE = new Color(240, 240, 240);
    
    // Animation
    private Timer celebrationTimer;
    private JLabel celebrationLabel;

    public FoodFlexGUI() {
        setupUI();
        setTitle("🍔 FoodFlex Pro MAX - Premium Food Delivery");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        // Main panel with dark background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(DARK_BG);

        // ========== RESTAURANT SELECTOR ==========
        JPanel restaurantPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        restaurantPanel.setBackground(DARK_BG);
        
        restaurantLabel = new JLabel("🏠 Current Restaurant: ");
        restaurantLabel.setForeground(TEXT_WHITE);
        restaurantLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JComboBox<Restaurant> restaurantCombo = new JComboBox<>(menuManager.getRestaurants().toArray(new Restaurant[0]));
        restaurantCombo.setRenderer(new RestaurantRenderer());
        restaurantCombo.addActionListener(e -> {
            currentRestaurant = (Restaurant) restaurantCombo.getSelectedItem();
            restaurantLabel.setText("🏠 " + currentRestaurant.getName() + " (" + currentRestaurant.getCuisine() + ")");
            updateMenuForRestaurant();
        });
        
        restaurantPanel.add(restaurantLabel);
        restaurantPanel.add(restaurantCombo);
        
        // Set default restaurant
        currentRestaurant = menuManager.getRestaurants().get(0);
        restaurantLabel.setText("🏠 " + currentRestaurant.getName() + " (" + currentRestaurant.getCuisine() + ")");

        // ========== MENU TABS ==========
        menuTabs = new JTabbedPane();
        menuTabs.setBackground(DARK_BG);
        menuTabs.setForeground(TEXT_WHITE);
        updateMenuForRestaurant();

        // ========== CART PANEL ==========
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT, 1), 
            "🛒 Your Cart", 
            0, 0, 
            new Font("Arial", Font.BOLD, 14), ACCENT
        ));
        cartPanel.setBackground(DARK_BG);
        cartPanel.setForeground(TEXT_WHITE);

        // Cart list with custom renderer
        cartList = new JList<>(cartModel);
        cartList.setCellRenderer(new CartItemRenderer());
        cartList.setBackground(CARD_BG);
        cartList.setForeground(TEXT_WHITE);
        cartList.setSelectionBackground(ACCENT);
        cartList.setVisibleRowCount(10);
        
        JScrollPane cartScroll = new JScrollPane(cartList);
        cartScroll.setBorder(BorderFactory.createEmptyBorder());
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        // Cart bottom panel
        JPanel cartBottom = new JPanel(new GridLayout(5, 1, 5, 5));
        cartBottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        cartBottom.setBackground(DARK_BG);

        // Calorie counter
        calorieLabel = new JLabel("🔥 Total Calories: 0");
        calorieLabel.setForeground(TEXT_WHITE);
        calorieLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Total label
        totalLabel = new JLabel("Total: ₹0.00 (Delivery: ₹0)");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setForeground(TEXT_WHITE);

        // Remove button
        JButton removeBtn = new JButton("❌ Remove Selected");
        styleButton(removeBtn, new Color(200, 50, 50));
        removeBtn.addActionListener(e -> removeFromCart());

        // Order button
        JButton orderBtn = new JButton("🚀 PLACE ORDER");
        styleButton(orderBtn, ACCENT);
        orderBtn.addActionListener(e -> placeOrder());
        
        // AI Recommendation button
        JButton aiBtn = new JButton("🤖 Get Recommendations");
        styleButton(aiBtn, new Color(100, 150, 255));
        aiBtn.addActionListener(e -> showAIRecommendations());
        
        cartBottom.add(calorieLabel);
        cartBottom.add(totalLabel);
        cartBottom.add(removeBtn);
        cartBottom.add(orderBtn);
        cartBottom.add(aiBtn);

        cartPanel.add(cartBottom, BorderLayout.SOUTH);

        // ========== STATUS PANEL ==========
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT, 1), 
            "📦 Order Status", 
            0, 0, 
            new Font("Arial", Font.BOLD, 14), ACCENT
        ));
        statusPanel.setBackground(DARK_BG);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(CARD_BG);
        progressBar.setFont(new Font("Arial", Font.BOLD, 12));
        statusPanel.add(progressBar);

        // Celebration label (hidden by default)
        celebrationLabel = new JLabel("", JLabel.CENTER);
        celebrationLabel.setFont(new Font("Arial", Font.BOLD, 24));
        celebrationLabel.setForeground(ACCENT);
        celebrationLabel.setVisible(false);

        // ========== MAIN LAYOUT ==========
        JSplitPane mainSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, 
            menuTabs, 
            cartPanel
        );
        mainSplit.setResizeWeight(0.65);
        mainSplit.setDividerSize(3);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane verticalSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, 
            mainSplit, 
            statusPanel
        );
        verticalSplit.setResizeWeight(0.8);
        verticalSplit.setDividerSize(3);

        mainPanel.add(restaurantPanel, BorderLayout.NORTH);
        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        mainPanel.add(celebrationLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private void updateMenuForRestaurant() {
        menuTabs.removeAll();
        
        // Best Sellers Tab
        JPanel bestSellersPanel = createBestSellersPanel();
        menuTabs.addTab("🏆 Best Sellers", bestSellersPanel);
        
        // Starters Tab
        JPanel startersPanel = createMenuPanel(Starter.class);
        menuTabs.addTab("🍟 Starters", startersPanel);
        
        // Main Courses Tab
        JPanel mainCoursePanel = createMenuPanel(MainCourse.class);
        menuTabs.addTab("🍗 Main Course", mainCoursePanel);
        
        // Desserts Tab
        JPanel dessertsPanel = createMenuPanel(Dessert.class);
        menuTabs.addTab("🍰 Desserts", dessertsPanel);
        
        // Beverages Tab
        JPanel beveragesPanel = createMenuPanel(Beverage.class);
        menuTabs.addTab("🥤 Beverages", beveragesPanel);
        
        revalidate();
        repaint();
    }

    private JPanel createBestSellersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_BG);

        // Get top 5 best sellers (sorted by price as a placeholder for popularity)
        List<Meal> bestSellers = new ArrayList<>(currentRestaurant.getMenu());
        bestSellers.sort((m1, m2) -> Double.compare(m2.getPrice(), m1.getPrice()));
        bestSellers = bestSellers.subList(0, Math.min(5, bestSellers.size()));

        DefaultListModel<Meal> model = new DefaultListModel<>();
        bestSellers.forEach(model::addElement);

        JList<Meal> bestSellerList = new JList<>(model);
        bestSellerList.setCellRenderer(new MenuItemRenderer());
        bestSellerList.setBackground(CARD_BG);
        bestSellerList.setSelectionBackground(ACCENT);
        bestSellerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bestSellerList.setVisibleRowCount(5);

        JScrollPane scroll = new JScrollPane(bestSellerList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        // Add to cart button
        JButton addBtn = new JButton("➕ ADD TO CART");
        styleButton(addBtn, new Color(70, 180, 80));
        addBtn.addActionListener(e -> addToCart(bestSellerList));
        panel.add(addBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMenuPanel(Class<?> mealClass) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_BG);

        // Menu items list
        DefaultListModel<Meal> model = new DefaultListModel<>();
        model.ensureCapacity(30); // Pre-allocate space for better performance
        
        currentRestaurant.getMenu().stream()
            .filter(item -> mealClass.isInstance(item))
            .forEach(model::addElement);

        JList<Meal> menuList = new JList<>(model);
        menuList.setCellRenderer(new MenuItemRenderer());
        menuList.setBackground(CARD_BG);
        menuList.setSelectionBackground(ACCENT);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setVisibleRowCount(12);

        JScrollPane scroll = new JScrollPane(menuList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        // Add to cart button
        JButton addBtn = new JButton("➕ ADD TO CART");
        styleButton(addBtn, new Color(70, 180, 80));
        addBtn.addActionListener(e -> addToCart(menuList));
        panel.add(addBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
    }

    private void addToCart(JList<Meal> sourceList) {
        Meal selected = sourceList.getSelectedValue();
        if (selected != null) {
            try {
                cart.addItem(selected);
                cartModel.addElement(selected);
                updateCartDetails();
                
                // Visual feedback
                JOptionPane.showMessageDialog(this, 
                    "Added " + selected.getName() + " to cart!", 
                    "🍽️ Item Added", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (ItemUnavailableException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "⚠️ Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeFromCart() {
        Meal selected = cartList.getSelectedValue();
        if (selected != null) {
            cart.removeItem(selected);
            cartModel.removeElement(selected);
            updateCartDetails();
            
            JOptionPane.showMessageDialog(this,
                "Removed " + selected.getName() + " from cart!",
                "🗑️ Item Removed",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select an item to remove!",
                "⚠️ Error",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateCartDetails() {
        NumberFormat rupee = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        double total = cart.getTotalPrice() + currentRestaurant.getDeliveryFee();
        totalLabel.setText("Total: " + rupee.format(total) + 
                         " (Delivery: " + rupee.format(currentRestaurant.getDeliveryFee()) + ")");
        
        int totalCalories = cart.getItems().stream().mapToInt(Meal::getCalories).sum();
        calorieLabel.setText("🔥 Total Calories: " + totalCalories);
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Your cart is empty! Add some delicious items first.", 
                "🛒 Empty Cart", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Start celebration animation
        startCelebration();
        
        // Create and process order
        Order order = new Order(cart.getItems(), currentRestaurant);
        new OrderProcessor(order, this).start();
        
        // Save to order history
        saveOrderToHistory(order);
        
        // Clear cart
        cart.clear();
        cartModel.clear();
        updateCartDetails();
    }

    private void startCelebration() {
        celebrationLabel.setText("🎉 Order Placed! 🎉");
        celebrationLabel.setVisible(true);
        
        celebrationTimer = new Timer(2000, e -> {
            celebrationLabel.setVisible(false);
            celebrationTimer.stop();
        });
        celebrationTimer.setRepeats(false);
        celebrationTimer.start();
    }

    private void saveOrderToHistory(Order order) {
        try (FileWriter fw = new FileWriter("order_history.txt", true)) {
            fw.write("Order #" + order.getOrderId() + 
                     " | Restaurant: " + order.getRestaurant().getName() +
                     " | Total: ₹" + String.format("%.2f", order.getTotalPrice()) +
                     " | Time: " + new Date() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAIRecommendations() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Add some items to your cart first for recommendations!",
                "🤖 AI Suggestion",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Simple AI logic based on cart items
        String recommendation;
        if (cart.getItems().stream().anyMatch(m -> m instanceof MainCourse)) {
            recommendation = "Try our Masala Lemonade with your meal!";
        } else if (cart.getItems().stream().anyMatch(m -> m instanceof Dessert)) {
            recommendation = "How about a hot Masala Chai to go with your sweets?";
        } else {
            recommendation = "Our Butter Chicken is today's special!";
        }
        
        JOptionPane.showMessageDialog(this,
            "🤖 FoodFlex AI Recommends:\n\n" + recommendation +
            "\n\n🍽️ Based on your current cart items",
            "✨ Personalized Recommendation",
            JOptionPane.PLAIN_MESSAGE);
    }

    // ========== ORDER STATUS UPDATES ==========
    @Override
    public void onOrderStarted(Order order) {
        progressBar.setValue(0);
        progressBar.setString("👨‍🍳 Preparing order #" + order.getOrderId() + " at " + order.getRestaurant().getName());
    }

    @Override
    public void onOrderProgress(Order order, int progress) {
        progressBar.setValue(progress);
        
        // Simulate rider tracking at 50% progress
        if (progress == 50) {
            progressBar.setString("🛵 Rider picked up your order! ETA: 20 mins");
        }
    }

    @Override
    public void onOrderCompleted(Order order) {
        progressBar.setString(order.getStatus().getDisplayText());
        if (order.getStatus() == Order.OrderStatus.READY) {
            progressBar.setValue(100);
            // Show delivery partner info
            String[] deliveryPartners = {"Rajesh (★★★★☆)", "Priya (★★★★★)", "Amit (★★★☆☆)", "Neha (★★★★☆)"};
            String deliveryPartner = deliveryPartners[new Random().nextInt(deliveryPartners.length)];
            
            JOptionPane.showMessageDialog(this, 
                "🎉 Order #" + order.getOrderId() + " is ready!\n" +
                "Estimated delivery time: 20 mins\n" +
                "Rider: " + deliveryPartner,
                "✅ Order Complete", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ========== CUSTOM RENDERERS ==========
    class MenuItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Meal) {
                Meal meal = (Meal) value;
                setText("<html><b>" + meal.getName() + "</b> - <font color='#FF6932'>" + 
                       formatRupee(meal.getPrice()) + "</font><br>" +
                       "<small>" + meal.getDescription() + "</small><br>" +
                       "<font color='#AAAAAA' size='2'>🔥 " + meal.getCalories() + " cal</font></html>");
            }
            
            setBackground(isSelected ? ACCENT : CARD_BG);
            setForeground(TEXT_WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            return this;
        }
    }

    class CartItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Meal) {
                Meal meal = (Meal) value;
                setText("<html><b>" + meal.getName() + "</b> - " + 
                       formatRupee(meal.getPrice()) + " (" + meal.getCalories() + " cal)</html>");
            }
            
            setBackground(isSelected ? ACCENT : CARD_BG);
            setForeground(TEXT_WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            return this;
        }
    }

    class RestaurantRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Restaurant) {
                Restaurant r = (Restaurant) value;
                setText("<html><b>" + r.getName() + "</b> (" + r.getCuisine() + ")<br>" +
                       "<small>🛵 " + formatRupee(r.getDeliveryFee()) + " delivery | ★" + 
                       String.format("%.1f", r.getRating()) + "</small></html>");
            }
            
            setBackground(isSelected ? ACCENT : CARD_BG);
            setForeground(TEXT_WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            return this;
        }
    }

    private String formatRupee(double amount) {
        return "₹" + String.format("%.2f", amount);
    }
}

// ================== ENHANCED FOOD ITEMS ==================
abstract class Meal {
    private final String id, name;
    private final double price;
    private final int prepTimeSeconds;
    private final int calories;
    private boolean available;
    private String description;

    public Meal(String id, String name, double price, int prepTimeSeconds, 
               boolean available, String description, int calories) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.prepTimeSeconds = prepTimeSeconds;
        this.available = available;
        this.description = description;
        this.calories = calories;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getPrepTimeSeconds() { return prepTimeSeconds; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getDescription() { return description; }
    public int getCalories() { return calories; }
}

class Starter extends Meal {
    public Starter(String id, String name, double price, int prepTime, 
                  boolean available, String description, int calories) {
        super(id, name, price, prepTime, available, "🍢 " + description, calories);
    }
}

class MainCourse extends Meal {
    public MainCourse(String id, String name, double price, int prepTime, 
                     boolean available, String description, int calories) {
        super(id, name, price, prepTime, available, "🍛 " + description, calories);
    }
}

class Dessert extends Meal {
    public Dessert(String id, String name, double price, int prepTime, 
                  boolean available, String description, int calories) {
        super(id, name, price, prepTime, available, "🍰 " + description, calories);
    }
}

class Beverage extends Meal {
    public Beverage(String id, String name, double price, int prepTime, 
                   boolean available, String description, int calories) {
        super(id, name, price, prepTime, available, "🥤 " + description, calories);
    }
}

// ================== RESTAURANT CLASS ==================
class Restaurant {
    private final String id;
    private final String name;
    private final String cuisine;
    private final double deliveryFee;
    private final List<Meal> menu;
    private final double rating;

    public Restaurant(String id, String name, String cuisine, double deliveryFee) {
        this.id = id;
        this.name = name;
        this.cuisine = cuisine;
        this.deliveryFee = deliveryFee;
        this.menu = new ArrayList<>();
        this.rating = 4.0 + new Random().nextDouble(); // Random rating 4.0-5.0
        
        initializeMenu();
    }

    private void initializeMenu() {
        switch (cuisine) {
            case "Indian":
                addIndianMenu();
                break;
            case "Italian":
                addItalianMenu();
                break;
            case "Japanese":
                addJapaneseMenu();
                break;
            case "American":
                addAmericanMenu();
                break;
            case "Mexican":
                addMexicanMenu();
                break;
            case "Chinese":
                addChineseMenu();
                break;
            case "Mediterranean":
                addMediterraneanMenu();
                break;
            case "BBQ":
                addBBQMenu();
                break;
            case "French":
                addFrenchMenu();
                break;
        }
    }

    private void addIndianMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST001", "Samosa", 49, 2, true, "Spiced potato filling", 150),
            new Starter("ST002", "Paneer Tikka", 129, 3, true, "Grilled cottage cheese", 180),
            new Starter("ST003", "Chicken 65", 149, 3, true, "Spicy fried chicken", 220),
            new Starter("ST004", "Aloo Tikki", 79, 2, true, "Potato patties", 120),
            new Starter("ST005", "Vegetable Pakora", 89, 3, true, "Fried vegetable fritters", 160),
            new Starter("ST006", "Gobi Manchurian", 139, 3, true, "Crispy cauliflower", 200),
            new Starter("ST007", "Fish Amritsari", 199, 4, true, "Punjabi style fish", 250),
            new Starter("ST008", "Papdi Chaat", 99, 2, true, "Street style chaat", 180),
            new Starter("ST009", "Dahi Puri", 119, 3, true, "Crispy puris with yogurt", 200),
            new Starter("ST010", "Bhel Puri", 89, 2, true, "Puffed rice snack", 150),
            
            new MainCourse("MC001", "Butter Chicken", 349, 5, true, "Creamy tomato chicken", 450),
            new MainCourse("MC002", "Palak Paneer", 279, 4, true, "Spinach with cottage cheese", 320),
            new MainCourse("MC003", "Chana Masala", 229, 4, true, "Spiced chickpeas", 280),
            new MainCourse("MC004", "Rogan Josh", 399, 6, true, "Kashmiri lamb curry", 500),
            new MainCourse("MC005", "Dal Tadka", 199, 3, true, "Tempered lentils", 250),
            new MainCourse("MC006", "Vegetable Biryani", 299, 5, true, "Fragrant rice with veggies", 380),
            new MainCourse("MC007", "Chicken Tikka Masala", 329, 5, true, "Grilled chicken in gravy", 420),
            new MainCourse("MC008", "Malai Kofta", 259, 4, true, "Vegetable balls in cream sauce", 350),
            new MainCourse("MC009", "Fish Curry", 379, 5, true, "South Indian style fish", 400),
            new MainCourse("MC010", "Prawn Masala", 429, 6, true, "Spicy prawn curry", 450),
            new MainCourse("MC011", "Hyderabadi Biryani", 349, 6, true, "Fragrant rice with meat", 480),
            new MainCourse("MC012", "Rajma Chawal", 229, 4, true, "Kidney beans with rice", 350),
            new MainCourse("MC013", "Mutton Korma", 449, 7, true, "Rich mutton curry", 520),
            new MainCourse("MC014", "Dosa", 159, 3, true, "South Indian crepe", 280),
            new MainCourse("MC015", "Aloo Paratha", 129, 3, true, "Stuffed flatbread", 320),
            new MainCourse("MC016", "Kadai Paneer", 299, 4, true, "Cottage cheese in spicy gravy", 380),
            new MainCourse("MC017", "Baingan Bharta", 239, 4, true, "Smoked eggplant curry", 300),
            new MainCourse("MC018", "Sambar Rice", 189, 3, true, "Lentil stew with rice", 350),
            new MainCourse("MC019", "Pav Bhaji", 199, 4, true, "Spiced vegetable mash with bread", 400),
            new MainCourse("MC020", "Keema Matar", 349, 5, true, "Minced meat with peas", 450),
            
            new Dessert("DS001", "Gulab Jamun", 99, 2, true, "Sweet milk balls", 250),
            new Dessert("DS002", "Rasmalai", 119, 2, true, "Cottage cheese in milk", 220),
            new Dessert("DS003", "Kheer", 89, 3, true, "Rice pudding", 180),
            new Dessert("DS004", "Gajar Halwa", 129, 4, true, "Carrot pudding", 210),
            new Dessert("DS005", "Jalebi", 69, 2, true, "Crispy sweet spirals", 280),
            new Dessert("DS006", "Rasgulla", 79, 2, true, "Spongy cottage cheese balls", 190),
            new Dessert("DS007", "Shrikhand", 109, 2, true, "Sweet strained yogurt", 170),
            new Dessert("DS008", "Malpua", 119, 3, true, "Sweet pancake", 230),
            new Dessert("DS009", "Besan Ladoo", 89, 2, true, "Chickpea flour sweets", 200),
            new Dessert("DS010", "Peda", 99, 2, true, "Milk-based sweet", 180),
            
            new Beverage("BV001", "Mango Lassi", 79, 1, true, "Yogurt mango drink", 200),
            new Beverage("BV002", "Masala Chai", 49, 2, true, "Spiced Indian tea", 80),
            new Beverage("BV003", "Badam Milk", 89, 2, true, "Almond flavored milk", 180),
            new Beverage("BV004", "Nimbu Pani", 39, 1, true, "Fresh lime water", 50),
            new Beverage("BV005", "Thandai", 99, 3, true, "Festive nut milk", 220),
            new Beverage("BV006", "Filter Coffee", 59, 2, true, "South Indian coffee", 60),
            new Beverage("BV007", "Rose Milk", 69, 1, true, "Rose flavored milk", 150),
            new Beverage("BV008", "Sugarcane Juice", 49, 1, true, "Fresh sugarcane", 120),
            new Beverage("BV009", "Aam Panna", 79, 2, true, "Raw mango drink", 90),
            new Beverage("BV010", "Paan Shots", 129, 3, true, "Digestive betel shot", 40),
            new Beverage("BV011", "Cold Coffee", 89, 2, true, "Iced coffee drink", 150),
            new Beverage("BV012", "Jaljeera", 59, 1, true, "Spiced cumin water", 30)
        ));
    }

    private void addItalianMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST011", "Bruschetta", 199, 3, true, "Toasted bread with tomatoes", 180),
            new Starter("ST012", "Caprese Salad", 229, 2, true, "Tomato mozzarella salad", 150),
            new Starter("ST013", "Garlic Bread", 129, 2, true, "Freshly baked with garlic butter", 220),
            new Starter("ST014", "Arancini", 179, 4, true, "Fried risotto balls", 250),
            new Starter("ST015", "Prosciutto e Melone", 299, 3, true, "Ham with melon", 180),
            new Starter("ST016", "Antipasto Platter", 349, 5, true, "Cured meats and cheeses", 320),
            new Starter("ST017", "Calamari Fritti", 279, 4, true, "Fried squid", 230),
            new Starter("ST018", "Mozzarella Sticks", 189, 3, true, "Fried cheese sticks", 280),
            new Starter("ST019", "Stuffed Mushrooms", 219, 4, true, "Baked stuffed mushrooms", 200),
            new Starter("ST020", "Focaccia Bread", 159, 3, true, "Italian flatbread", 250),
            
            new MainCourse("MC021", "Spaghetti Carbonara", 399, 6, true, "Creamy pasta with bacon", 550),
            new MainCourse("MC022", "Margherita Pizza", 349, 5, true, "Classic tomato and cheese", 480),
            new MainCourse("MC023", "Lasagna", 379, 7, true, "Layered pasta dish", 520),
            new MainCourse("MC024", "Risotto ai Funghi", 329, 6, true, "Mushroom risotto", 420),
            new MainCourse("MC025", "Chicken Parmigiana", 429, 7, true, "Breaded chicken with cheese", 580),
            new MainCourse("MC026", "Fettuccine Alfredo", 359, 5, true, "Creamy pasta", 490),
            new MainCourse("MC027", "Osso Buco", 499, 8, true, "Braised veal shanks", 620),
            new MainCourse("MC028", "Eggplant Parmigiana", 299, 5, true, "Baked eggplant with cheese", 380),
            new MainCourse("MC029", "Penne Arrabbiata", 279, 4, true, "Spicy tomato pasta", 350),
            new MainCourse("MC030", "Gnocchi Sorrentina", 319, 5, true, "Potato dumplings with tomato", 400),
            new MainCourse("MC031", "Veal Marsala", 459, 7, true, "Veal in mushroom wine sauce", 540),
            new MainCourse("MC032", "Seafood Linguine", 479, 7, true, "Pasta with mixed seafood", 520),
            new MainCourse("MC033", "Pizza Quattro Formaggi", 389, 6, true, "Four cheese pizza", 500),
            new MainCourse("MC034", "Ravioli Spinaci", 339, 5, true, "Spinach stuffed pasta", 380),
            new MainCourse("MC035", "Saltimbocca", 439, 6, true, "Roman style veal", 480),
            new MainCourse("MC036", "Pizza Diavola", 369, 5, true, "Spicy salami pizza", 450),
            new MainCourse("MC037", "Spaghetti Vongole", 419, 6, true, "Clam pasta", 400),
            new MainCourse("MC038", "Pizza Capricciosa", 399, 6, true, "Assorted toppings pizza", 480),
            new MainCourse("MC039", "Tagliatelle Bolognese", 379, 6, true, "Meat sauce pasta", 500),
            new MainCourse("MC040", "Pizza Prosciutto", 389, 5, true, "Ham and mushroom pizza", 450),
            
            new Dessert("DS011", "Tiramisu", 169, 2, true, "Coffee-flavored dessert", 320),
            new Dessert("DS012", "Panna Cotta", 149, 2, true, "Creamy Italian dessert", 280),
            new Dessert("DS013", "Cannoli", 129, 2, true, "Crispy pastry tubes", 220),
            new Dessert("DS014", "Gelato", 99, 1, true, "Italian ice cream", 180),
            new Dessert("DS015", "Affogato", 119, 2, true, "Ice cream with espresso", 150),
            new Dessert("DS016", "Zabaglione", 159, 3, true, "Egg custard dessert", 200),
            new Dessert("DS017", "Panettone", 179, 2, true, "Italian sweet bread", 300),
            new Dessert("DS018", "Sfogliatella", 139, 2, true, "Layered pastry", 240),
            new Dessert("DS019", "Semifreddo", 149, 2, true, "Semi-frozen dessert", 220),
            new Dessert("DS020", "Biscotti", 89, 1, true, "Italian almond cookies", 120),
            
            new Beverage("BV021", "Red Wine", 299, 1, true, "House special", 120),
            new Beverage("BV022", "White Wine", 279, 1, true, "Chardonnay", 110),
            new Beverage("BV023", "Limoncello", 199, 1, true, "Lemon liqueur", 80),
            new Beverage("BV024", "Espresso", 69, 1, true, "Strong Italian coffee", 5),
            new Beverage("BV025", "Cappuccino", 89, 2, true, "Espresso with milk foam", 120),
            new Beverage("BV026", "Aperol Spritz", 229, 2, true, "Popular Italian cocktail", 150),
            new Beverage("BV027", "Negroni", 249, 2, true, "Classic Italian cocktail", 130),
            new Beverage("BV028", "San Pellegrino", 59, 1, true, "Sparkling water", 0),
            new Beverage("BV029", "Italian Soda", 99, 1, true, "Fruit flavored soda", 140),
            new Beverage("BV030", "Grappa", 179, 1, true, "Italian grape brandy", 90),
            new Beverage("BV031", "Americano", 79, 1, true, "Espresso with hot water", 10),
            new Beverage("BV032", "Macchiato", 89, 1, true, "Espresso with milk", 50)
        ));
    }

    private void addJapaneseMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST021", "Edamame", 129, 2, true, "Steamed soybeans", 120),
            new Starter("ST022", "Miso Soup", 99, 2, true, "Traditional Japanese soup", 70),
            new Starter("ST023", "Agedashi Tofu", 159, 3, true, "Fried tofu in broth", 180),
            new Starter("ST024", "Gyoza", 179, 4, true, "Japanese dumplings", 200),
            new Starter("ST025", "Takoyaki", 199, 4, true, "Octopus balls", 220),
            new Starter("ST026", "Sunomono", 139, 2, true, "Cucumber salad", 90),
            new Starter("ST027", "Ebi Tempura", 239, 4, true, "Fried shrimp", 250),
            new Starter("ST028", "Yakitori", 189, 3, true, "Grilled chicken skewers", 180),
            new Starter("ST029", "Sashimi Appetizer", 299, 3, true, "Assorted raw fish slices", 150),
            new Starter("ST030", "Chawanmushi", 179, 3, true, "Savory egg custard", 130),
            
            new MainCourse("MC041", "California Roll", 349, 4, true, "Crab avocado roll", 320),
            new MainCourse("MC042", "Spicy Tuna Roll", 379, 4, true, "Tuna with spicy mayo", 350),
            new MainCourse("MC043", "Chicken Teriyaki", 329, 5, true, "Grilled chicken with sauce", 380),
            new MainCourse("MC044", "Beef Yakiniku", 429, 6, true, "Grilled beef", 450),
            new MainCourse("MC045", "Tonkatsu", 359, 5, true, "Breaded pork cutlet", 420),
            new MainCourse("MC046", "Ramen", 299, 4, true, "Japanese noodle soup", 480),
            new MainCourse("MC047", "Udon", 279, 4, true, "Thick wheat noodles", 400),
            new MainCourse("MC048", "Sashimi Platter", 499, 5, true, "Assorted raw fish", 350),
            new MainCourse("MC049", "Unagi Don", 399, 5, true, "Eel rice bowl", 450),
            new MainCourse("MC050", "Okonomiyaki", 329, 6, true, "Japanese savory pancake", 380),
            new MainCourse("MC051", "Sukiyaki", 459, 7, true, "Hot pot with beef", 520),
            new MainCourse("MC052", "Tempura Udon", 349, 5, true, "Noodles with tempura", 450),
            new MainCourse("MC053", "Chirashi Bowl", 379, 5, true, "Scattered sushi bowl", 400),
            new MainCourse("MC054", "Katsu Curry", 359, 5, true, "Cutlet with curry", 480),
            new MainCourse("MC055", "Yakisoba", 299, 4, true, "Stir-fried noodles", 350),
            new MainCourse("MC056", "Dragon Roll", 419, 5, true, "Eel and cucumber roll", 380),
            new MainCourse("MC057", "Rainbow Roll", 399, 5, true, "Assorted fish roll", 360),
            new MainCourse("MC058", "Beef Sukiyaki Don", 389, 6, true, "Beef hot pot rice bowl", 450),
            new MainCourse("MC059", "Salmon Teriyaki", 349, 5, true, "Grilled salmon with sauce", 400),
            new MainCourse("MC060", "Vegetable Tempura", 279, 4, true, "Assorted fried vegetables", 320),
            
            new Dessert("DS021", "Mochi Ice Cream", 149, 2, true, "Rice cake with ice cream", 180),
            new Dessert("DS022", "Matcha Tiramisu", 169, 2, true, "Green tea flavored dessert", 220),
            new Dessert("DS023", "Dorayaki", 129, 2, true, "Red bean pancake", 200),
            new Dessert("DS024", "Taiyaki", 119, 2, true, "Fish-shaped cake", 180),
            new Dessert("DS025", "Matcha Parfait", 179, 3, true, "Green tea sundae", 250),
            new Dessert("DS026", "Anmitsu", 159, 3, true, "Agar jelly dessert", 200),
            new Dessert("DS027", "Warabi Mochi", 139, 2, true, "Jelly-like confection", 160),
            new Dessert("DS028", "Castella", 149, 2, true, "Japanese sponge cake", 220),
            new Dessert("DS029", "Yokan", 129, 2, true, "Red bean jelly", 150),
            new Dessert("DS030", "Zenzai", 139, 3, true, "Sweet red bean soup", 180),
            
            new Beverage("BV041", "Matcha Latte", 129, 2, true, "Green tea with milk", 120),
            new Beverage("BV042", "Sake", 199, 1, true, "Japanese rice wine", 150),
            new Beverage("BV043", "Umeshu", 179, 1, true, "Plum wine", 130),
            new Beverage("BV044", "Ramune", 99, 1, true, "Japanese soda", 140),
            new Beverage("BV045", "Hojicha Tea", 89, 1, true, "Roasted green tea", 10),
            new Beverage("BV046", "Genmaicha Tea", 89, 1, true, "Brown rice tea", 10),
            new Beverage("BV047", "Calpico", 109, 1, true, "Japanese soft drink", 110),
            new Beverage("BV048", "Melon Soda", 119, 1, true, "Green melon flavored", 130),
            new Beverage("BV049", "Shiso Juice", 99, 1, true, "Perilla leaf drink", 50),
            new Beverage("BV050", "Yuzu Tea", 109, 1, true, "Citrus herbal tea", 30),
            new Beverage("BV051", "Cold Brew Tea", 99, 1, true, "Iced green tea", 5),
            new Beverage("BV052", "Shochu", 219, 1, true, "Japanese distilled beverage", 120)
        ));
    }

    private void addAmericanMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST031", "Buffalo Wings", 249, 4, true, "Spicy chicken wings", 350),
            new Starter("ST032", "Mozzarella Sticks", 189, 3, true, "Fried cheese sticks", 280),
            new Starter("ST033", "Nachos", 219, 3, true, "Tortilla chips with toppings", 400),
            new Starter("ST034", "Onion Rings", 179, 3, true, "Fried onion rings", 320),
            new Starter("ST035", "Spinach Artichoke Dip", 199, 4, true, "Creamy dip with chips", 380),
            new Starter("ST036", "Chicken Quesadilla", 229, 4, true, "Grilled chicken and cheese", 420),
            new Starter("ST037", "Potato Skins", 209, 4, true, "Loaded potato halves", 350),
            new Starter("ST038", "Bacon Cheese Fries", 239, 4, true, "Fries with bacon and cheese", 450),
            new Starter("ST039", "Clam Chowder", 189, 3, true, "Creamy seafood soup", 280),
            new Starter("ST040", "Fried Pickles", 159, 3, true, "Battered fried pickles", 250),
            
            new MainCourse("MC061", "Cheeseburger", 299, 5, true, "Classic beef burger", 550),
            new MainCourse("MC062", "BBQ Ribs", 449, 6, true, "Slow-cooked pork ribs", 680),
            new MainCourse("MC063", "Fried Chicken", 329, 5, true, "Southern style chicken", 600),
            new MainCourse("MC064", "Philly Cheesesteak", 379, 5, true, "Beef sandwich with cheese", 520),
            new MainCourse("MC065", "Mac & Cheese", 259, 4, true, "Creamy pasta dish", 450),
            new MainCourse("MC066", "Club Sandwich", 279, 4, true, "Triple-decker sandwich", 480),
            new MainCourse("MC067", "Steak", 599, 7, true, "Grilled beef steak", 700),
            new MainCourse("MC068", "Hot Dog", 199, 3, true, "Classic American hot dog", 350),
            new MainCourse("MC069", "Chicken Pot Pie", 329, 5, true, "Creamy chicken in pastry", 500),
            new MainCourse("MC070", "Meatloaf", 279, 5, true, "Homestyle meatloaf", 480),
            new MainCourse("MC071", "Reuben Sandwich", 299, 4, true, "Corned beef sandwich", 520),
            new MainCourse("MC072", "Pulled Pork Sandwich", 319, 5, true, "Slow-cooked pork", 480),
            new MainCourse("MC073", "Caesar Salad", 239, 3, true, "Romaine with dressing", 350),
            new MainCourse("MC074", "Fish and Chips", 349, 5, true, "Battered fish with fries", 550),
            new MainCourse("MC075", "Bacon Cheeseburger", 329, 5, true, "Burger with bacon", 600),
            new MainCourse("MC076", "Chicken Fried Steak", 359, 6, true, "Breaded steak with gravy", 650),
            new MainCourse("MC077", "Turkey Dinner", 399, 7, true, "Roast turkey with sides", 700),
            new MainCourse("MC078", "Lobster Roll", 499, 5, true, "Lobster meat sandwich", 450),
            new MainCourse("MC079", "Biscuits and Gravy", 229, 4, true, "Southern breakfast", 480),
            new MainCourse("MC080", "Chili Cheese Dog", 249, 4, true, "Hot dog with chili", 500),
            
            new Dessert("DS031", "Apple Pie", 149, 3, true, "Classic American pie", 350),
            new Dessert("DS032", "Cheesecake", 169, 2, true, "Creamy New York style", 400),
            new Dessert("DS033", "Chocolate Chip Cookies", 99, 2, true, "Fresh baked cookies", 200),
            new Dessert("DS034", "Brownie Sundae", 179, 3, true, "Warm brownie with ice cream", 450),
            new Dessert("DS035", "Banana Split", 199, 3, true, "Classic ice cream dessert", 500),
            new Dessert("DS036", "Pecan Pie", 159, 3, true, "Southern nut pie", 380),
            new Dessert("DS037", "Red Velvet Cake", 189, 2, true, "Southern specialty cake", 420),
            new Dessert("DS038", "Key Lime Pie", 149, 2, true, "Tangy citrus pie", 350),
            new Dessert("DS039", "Milkshake", 129, 2, true, "Thick creamy shake", 300),
            new Dessert("DS040", "S'mores", 139, 3, true, "Campfire classic", 250),
            
            new Beverage("BV061", "Root Beer Float", 149, 2, true, "Soda with ice cream", 250),
            new Beverage("BV062", "Iced Tea", 79, 1, true, "Sweet or unsweetened", 100),
            new Beverage("BV063", "Lemonade", 89, 1, true, "Fresh squeezed", 120),
            new Beverage("BV064", "Milkshake", 129, 2, true, "Vanilla, chocolate or strawberry", 300),
            new Beverage("BV065", "Soda", 59, 1, true, "Various flavors", 150),
            new Beverage("BV066", "Coffee", 69, 1, true, "Fresh brewed", 5),
            new Beverage("BV067", "Craft Beer", 199, 1, true, "Local selection", 150),
            new Beverage("BV068", "Bourbon", 179, 1, true, "Kentucky straight", 100),
            new Beverage("BV069", "Mint Julep", 189, 2, true, "Southern cocktail", 200),
            new Beverage("BV070", "Egg Cream", 109, 1, true, "New York classic", 180),
            new Beverage("BV071", "Arnold Palmer", 99, 1, true, "Half iced tea, half lemonade", 120),
            new Beverage("BV072", "Hot Chocolate", 119, 2, true, "Rich chocolate drink", 200)
        ));
    }

    private void addMexicanMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST041", "Guacamole", 179, 3, true, "Fresh avocado dip", 220),
            new Starter("ST042", "Queso Fundido", 199, 4, true, "Melted cheese dip", 280),
            new Starter("ST043", "Nachos", 219, 3, true, "Tortilla chips with toppings", 400),
            new Starter("ST044", "Elote", 149, 3, true, "Mexican street corn", 250),
            new Starter("ST045", "Tostadas", 169, 3, true, "Crispy corn tortillas", 200),
            new Starter("ST046", "Chorizo Quesadilla", 229, 4, true, "Spicy sausage and cheese", 350),
            new Starter("ST047", "Ceviche", 249, 4, true, "Citrus-marinated seafood", 180),
            new Starter("ST048", "Sopes", 189, 4, true, "Thick corn cakes", 280),
            new Starter("ST049", "Taco Salad", 199, 3, true, "Crispy shell with fillings", 380),
            new Starter("ST050", "Chiles Toreados", 159, 3, true, "Blistered peppers", 120),
            
            new MainCourse("MC081", "Tacos al Pastor", 249, 4, true, "Marinated pork tacos", 350),
            new MainCourse("MC082", "Enchiladas", 279, 5, true, "Stuffed tortillas with sauce", 420),
            new MainCourse("MC083", "Burrito", 299, 5, true, "Large flour tortilla wrap", 550),
            new MainCourse("MC084", "Chiles Rellenos", 259, 5, true, "Stuffed poblano peppers", 380),
            new MainCourse("MC085", "Mole Poblano", 329, 6, true, "Chicken in rich sauce", 450),
            new MainCourse("MC086", "Fajitas", 349, 6, true, "Sizzling grilled meat", 480),
            new MainCourse("MC087", "Tamales", 229, 5, true, "Steamed corn dough", 320),
            new MainCourse("MC088", "Pozole", 199, 4, true, "Hominy stew", 350),
            new MainCourse("MC089", "Carnitas", 299, 5, true, "Slow-cooked pork", 400),
            new MainCourse("MC090", "Quesadilla", 219, 4, true, "Grilled cheese tortilla", 380),
            new MainCourse("MC091", "Tlayuda", 259, 5, true, "Oaxacan pizza", 420),
            new MainCourse("MC092", "Birria Tacos", 279, 5, true, "Stewed meat tacos", 350),
            new MainCourse("MC093", "Pambazo", 229, 4, true, "Dipped bread sandwich", 400),
            new MainCourse("MC094", "Huarache", 239, 5, true, "Oval-shaped masa base", 380),
            new MainCourse("MC095", "Sopes", 199, 4, true, "Thick corn cakes", 320),
            new MainCourse("MC096", "Chilaquiles", 189, 4, true, "Fried tortilla dish", 350),
            new MainCourse("MC097", "Menudo", 209, 5, true, "Tripe soup", 300),
            new MainCourse("MC098", "Barbacoa", 319, 6, true, "Slow-cooked beef", 450),
            new MainCourse("MC099", "Pescado Zarandeado", 379, 6, true, "Grilled whole fish", 400),
            new MainCourse("MC100", "Carne Asada", 349, 5, true, "Grilled steak", 480),
            
            new Dessert("DS041", "Churros", 129, 3, true, "Fried dough pastry", 280),
            new Dessert("DS042", "Flan", 119, 2, true, "Caramel custard", 220),
            new Dessert("DS043", "Tres Leches Cake", 149, 2, true, "Three milk cake", 350),
            new Dessert("DS044", "Arroz con Leche", 99, 2, true, "Rice pudding", 250),
            new Dessert("DS045", "Pastel de Elote", 139, 3, true, "Corn cake", 300),
            new Dessert("DS046", "Cajeta Crepes", 159, 3, true, "Goat milk caramel crepes", 320),
            new Dessert("DS047", "Buñuelos", 109, 2, true, "Fried dough with syrup", 280),
            new Dessert("DS048", "Jericalla", 119, 2, true, "Mexican custard", 200),
            new Dessert("DS049", "Mangonada", 139, 3, true, "Mango sorbet with chili", 250),
            new Dessert("DS050", "Ate con Queso", 129, 2, true, "Fruit paste with cheese", 220),
            
            new Beverage("BV081", "Horchata", 99, 2, true, "Rice milk drink", 180),
            new Beverage("BV082", "Jamaica", 89, 1, true, "Hibiscus tea", 50),
            new Beverage("BV083", "Tamarindo", 99, 1, true, "Tamarind drink", 120),
            new Beverage("BV084", "Michelada", 179, 2, true, "Beer cocktail", 150),
            new Beverage("BV085", "Margarita", 199, 2, true, "Classic tequila cocktail", 200),
            new Beverage("BV086", "Paloma", 189, 2, true, "Grapefruit tequila drink", 180),
            new Beverage("BV087", "Tequila", 159, 1, true, "100% agave", 100),
            new Beverage("BV088", "Mezcal", 179, 1, true, "Smoky agave spirit", 110),
            new Beverage("BV089", "Atole", 109, 2, true, "Warm corn drink", 200),
            new Beverage("BV090", "Café de Olla", 89, 2, true, "Spiced Mexican coffee", 120),
            new Beverage("BV091", "Pulque", 149, 1, true, "Fermented agave drink", 150),
            new Beverage("BV092", "Mexican Hot Chocolate", 119, 2, true, "Spiced chocolate", 180)
        ));
    }

    private void addChineseMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST051", "Spring Rolls", 149, 3, true, "Crispy vegetable rolls", 200),
            new Starter("ST052", "Dumplings", 179, 4, true, "Steamed or fried", 250),
            new Starter("ST053", "Wonton Soup", 129, 3, true, "Pork dumpling soup", 180),
            new Starter("ST054", "Peking Duck Pancakes", 299, 4, true, "Thin pancakes with duck", 350),
            new Starter("ST055", "Scallion Pancakes", 159, 3, true, "Flaky layered bread", 280),
            new Starter("ST056", "Hot and Sour Soup", 139, 3, true, "Spicy tangy soup", 200),
            new Starter("ST057", "Egg Rolls", 169, 3, true, "Crispy fried rolls", 300),
            new Starter("ST058", "Szechuan Chicken", 199, 4, true, "Spicy appetizer", 250),
            new Starter("ST059", "Crab Rangoon", 189, 4, true, "Cream cheese wontons", 280),
            new Starter("ST060", "Spicy Cucumber Salad", 119, 2, true, "Refreshing side", 100),
            
            new MainCourse("MC101", "Kung Pao Chicken", 299, 5, true, "Spicy stir-fry", 450),
            new MainCourse("MC102", "Beef with Broccoli", 279, 4, true, "Classic stir-fry", 380),
            new MainCourse("MC103", "Sweet and Sour Pork", 259, 4, true, "Crispy pork in sauce", 420),
            new MainCourse("MC104", "Mapo Tofu", 239, 4, true, "Spicy tofu dish", 350),
            new MainCourse("MC105", "Peking Duck", 499, 6, true, "Roasted duck", 550),
            new MainCourse("MC106", "General Tso's Chicken", 289, 5, true, "Crispy chicken in sauce", 480),
            new MainCourse("MC107", "Moo Shu Pork", 269, 5, true, "Stir-fry with pancakes", 400),
            new MainCourse("MC108", "Szechuan Beef", 309, 5, true, "Spicy beef dish", 450),
            new MainCourse("MC109", "Honey Walnut Shrimp", 349, 5, true, "Crispy shrimp", 400),
            new MainCourse("MC110", "Chow Mein", 229, 4, true, "Stir-fried noodles", 380),
            new MainCourse("MC111", "Fried Rice", 219, 4, true, "Classic rice dish", 350),
            new MainCourse("MC112", "Orange Chicken", 279, 5, true, "Sweet citrus chicken", 420),
            new MainCourse("MC113", "Egg Foo Young", 239, 4, true, "Chinese omelette", 300),
            new MainCourse("MC114", "Char Siu Pork", 259, 5, true, "BBQ pork", 380),
            new MainCourse("MC115", "Hot Pot", 399, 6, true, "Interactive cooking", 500),
            new MainCourse("MC116", "Lo Mein", 229, 4, true, "Soft egg noodles", 350),
            new MainCourse("MC117", "Xiaolongbao", 249, 4, true, "Soup dumplings", 280),
            new MainCourse("MC118", "Dan Dan Noodles", 219, 4, true, "Spicy Sichuan noodles", 320),
            new MainCourse("MC119", "Salt and Pepper Shrimp", 329, 5, true, "Crispy seasoned shrimp", 350),
            new MainCourse("MC120", "Clay Pot Rice", 279, 5, true, "Rice cooked in clay pot", 400),
            
            new Dessert("DS051", "Mango Pudding", 119, 2, true, "Creamy mango dessert", 200),
            new Dessert("DS052", "Red Bean Bun", 99, 2, true, "Steamed sweet bun", 180),
            new Dessert("DS053", "Sesame Balls", 109, 3, true, "Fried glutinous rice", 220),
            new Dessert("DS054", "Egg Tarts", 129, 2, true, "Flaky pastry with custard", 250),
            new Dessert("DS055", "Almond Jelly", 89, 2, true, "Light almond dessert", 150),
            new Dessert("DS056", "Fortune Cookies", 59, 1, true, "Classic crispy cookies", 50),
            new Dessert("DS057", "Taro Cake", 119, 2, true, "Steamed root vegetable cake", 180),
            new Dessert("DS058", "Lychee with Ice Cream", 139, 2, true, "Tropical fruit dessert", 220),
            new Dessert("DS059", "Mooncake", 149, 2, true, "Festive pastry", 300),
            new Dessert("DS060", "Sweet Tofu Pudding", 99, 2, true, "Silky soybean dessert", 150),
            
            new Beverage("BV101", "Jasmine Tea", 69, 1, true, "Fragrant Chinese tea", 5),
            new Beverage("BV102", "Bubble Tea", 129, 2, true, "Milk tea with tapioca", 250),
            new Beverage("BV103", "Plum Juice", 89, 1, true, "Sweet-sour drink", 120),
            new Beverage("BV104", "Soy Milk", 79, 1, true, "Traditional drink", 100),
            new Beverage("BV105", "Lychee Martini", 199, 2, true, "Fruit cocktail", 180),
            new Beverage("BV106", "Oolong Tea", 79, 1, true, "Semi-oxidized tea", 5),
            new Beverage("BV107", "Honey Lemon Tea", 99, 1, true, "Soothing hot drink", 120),
            new Beverage("BV108", "Baijiu", 179, 1, true, "Chinese liquor", 150),
            new Beverage("BV109", "Winter Melon Tea", 89, 1, true, "Sweet herbal drink", 100),
            new Beverage("BV110", "Sour Plum Drink", 99, 1, true, "Refreshing beverage", 80),
            new Beverage("BV111", "Chrysanthemum Tea", 79, 1, true, "Floral herbal tea", 5),
            new Beverage("BV112", "Ginger Tea", 89, 1, true, "Spiced hot drink", 30)
        ));
    }

    private void addMediterraneanMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST061", "Hummus", 149, 2, true, "Chickpea dip", 200),
            new Starter("ST062", "Baba Ganoush", 159, 3, true, "Eggplant dip", 180),
            new Starter("ST063", "Tzatziki", 139, 2, true, "Yogurt cucumber dip", 150),
            new Starter("ST064", "Dolma", 169, 3, true, "Stuffed grape leaves", 220),
            new Starter("ST065", "Falafel", 179, 4, true, "Fried chickpea balls", 250),
            new Starter("ST066", "Spanakopita", 189, 3, true, "Spinach pie", 280),
            new Starter("ST067", "Tabouli", 129, 2, true, "Parsley salad", 120),
            new Starter("ST068", "Fattoush", 139, 2, true, "Bread salad", 180),
            new Starter("ST069", "Muhammara", 159, 3, true, "Red pepper dip", 200),
            new Starter("ST070", "Halloumi Fries", 199, 4, true, "Fried cheese sticks", 300),
            
            new MainCourse("MC121", "Shawarma", 299, 5, true, "Spiced meat wrap", 450),
            new MainCourse("MC122", "Gyro", 279, 4, true, "Meat with pita", 400),
            new MainCourse("MC123", "Moussaka", 329, 6, true, "Eggplant casserole", 480),
            new MainCourse("MC124", "Kebab Platter", 349, 5, true, "Grilled meat assortment", 500),
            new MainCourse("MC125", "Paella", 399, 7, true, "Spanish rice dish", 550),
            new MainCourse("MC126", "Tagine", 359, 6, true, "Slow-cooked stew", 480),
            new MainCourse("MC127", "Falafel Wrap", 239, 4, true, "Chickpea patty wrap", 380),
            new MainCourse("MC128", "Stuffed Peppers", 259, 5, true, "Bell peppers with rice", 350),
            new MainCourse("MC129", "Lamb Kofta", 299, 5, true, "Spiced meatballs", 420),
            new MainCourse("MC130", "Seafood Orzo", 349, 6, true, "Pasta with seafood", 450),
            new MainCourse("MC131", "Chicken Souvlaki", 279, 4, true, "Grilled chicken skewers", 380),
            new MainCourse("MC132", "Ratatouille", 239, 5, true, "Vegetable stew", 300),
            new MainCourse("MC133", "Grilled Octopus", 379, 6, true, "Tender seafood", 350),
            new MainCourse("MC134", "Lentil Soup", 189, 3, true, "Hearty legume soup", 250),
            new MainCourse("MC135", "Pide", 269, 5, true, "Turkish flatbread pizza", 400),
            new MainCourse("MC136", "Stuffed Eggplant", 249, 5, true, "Baked with fillings", 350),
            new MainCourse("MC137", "Greek Salad", 219, 3, true, "Fresh vegetable salad", 280),
            new MainCourse("MC138", "Baklava", 169, 2, true, "Sweet pastry", 320),
            new MainCourse("MC139", "Couscous", 199, 4, true, "Steamed semolina", 300),
            new MainCourse("MC140", "Grilled Fish", 349, 5, true, "Fresh seafood", 400),
            
            new Dessert("DS061", "Baklava", 149, 2, true, "Layered pastry with nuts", 350),
            new Dessert("DS062", "Kunafa", 169, 3, true, "Cheese pastry", 400),
            new Dessert("DS063", "Loukoumades", 139, 3, true, "Greek doughnuts", 300),
            new Dessert("DS064", "Halva", 119, 2, true, "Sesame sweet", 250),
            new Dessert("DS065", "Revani", 129, 2, true, "Semolina cake", 280),
            new Dessert("DS066", "Mahalabia", 109, 2, true, "Milk pudding", 200),
            new Dessert("DS067", "Sutlac", 119, 3, true, "Rice pudding", 220),
            new Dessert("DS068", "Galaktoboureko", 159, 3, true, "Custard pie", 350),
            new Dessert("DS069", "Qatayef", 139, 3, true, "Stuffed pancakes", 280),
            new Dessert("DS070", "Turkish Delight", 99, 1, true, "Chewy confection", 180),
            
            new Beverage("BV121", "Turkish Coffee", 89, 2, true, "Strong traditional coffee", 10),
            new Beverage("BV122", "Mint Tea", 79, 1, true, "Refreshing herbal tea", 5),
            new Beverage("BV123", "Ayran", 69, 1, true, "Yogurt drink", 100),
            new Beverage("BV124", "Arak", 179, 1, true, "Anise-flavored spirit", 120),
            new Beverage("BV125", "Pomegranate Juice", 99, 1, true, "Fresh squeezed", 120),
            new Beverage("BV126", "Sahlab", 109, 2, true, "Warm milk drink", 200),
            new Beverage("BV127", "Raki", 159, 1, true, "Turkish alcoholic drink", 130),
            new Beverage("BV128", "Lemonade with Mint", 89, 1, true, "Refreshing citrus drink", 120),
            new Beverage("BV129", "Rose Water", 79, 1, true, "Floral flavored water", 5),
            new Beverage("BV130", "Almond Milk", 99, 1, true, "Nutty dairy alternative", 150),
            new Beverage("BV131", "Sour Cherry Juice", 109, 1, true, "Tart fruit drink", 100),
            new Beverage("BV132", "Cardamom Coffee", 99, 2, true, "Spiced coffee", 10)
        ));
    }

    private void addBBQMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST071", "BBQ Wings", 229, 4, true, "Smoky chicken wings", 350),
            new Starter("ST072", "Pulled Pork Sliders", 249, 4, true, "Mini sandwiches", 300),
            new Starter("ST073", "Brisket Tacos", 269, 4, true, "Smoked meat tacos", 320),
            new Starter("ST074", "Jalapeño Poppers", 199, 3, true, "Stuffed peppers", 280),
            new Starter("ST075", "Smoked Sausage", 219, 3, true, "House-made links", 350),
            new Starter("ST076", "BBQ Nachos", 239, 4, true, "Loaded with meat", 450),
            new Starter("ST077", "Fried Pickles", 179, 3, true, "Battered and fried", 250),
            new Starter("ST078", "Deviled Eggs", 189, 2, true, "Classic Southern", 200),
            new Starter("ST079", "Cornbread", 159, 2, true, "Sweet Southern", 220),
            new Starter("ST080", "Collard Greens", 169, 3, true, "Slow-cooked greens", 150),
            
            new MainCourse("MC141", "Brisket Platter", 499, 6, true, "Slow-smoked beef", 600),
            new MainCourse("MC142", "Ribs Platter", 449, 5, true, "Fall-off-the-bone", 550),
            new MainCourse("MC143", "Pulled Pork", 379, 5, true, "Shredded pork", 500),
            new MainCourse("MC144", "Smoked Chicken", 349, 5, true, "Juicy and tender", 450),
            new MainCourse("MC145", "Burnt Ends", 429, 5, true, "Brisket pieces", 480),
            new MainCourse("MC146", "BBQ Sampler", 549, 6, true, "Assorted meats", 700),
            new MainCourse("MC147", "Beef Ribs", 499, 6, true, "Meaty ribs", 650),
            new MainCourse("MC148", "Smoked Turkey", 399, 5, true, "Juicy poultry", 400),
            new MainCourse("MC149", "BBQ Sandwich", 299, 4, true, "Pulled pork or chicken", 450),
            new MainCourse("MC150", "Sausage Platter", 329, 4, true, "Assorted smoked", 400),
            new MainCourse("MC151", "BBQ Burger", 349, 5, true, "With smoked meat", 550),
            new MainCourse("MC152", "Pork Belly", 379, 5, true, "Crispy and tender", 500),
            new MainCourse("MC153", "BBQ Tacos", 299, 4, true, "With choice of meat", 380),
            new MainCourse("MC154", "Smoked Meatloaf", 329, 5, true, "BBQ style", 450),
            new MainCourse("MC155", "BBQ Chicken", 279, 4, true, "Half or whole", 400),
            new MainCourse("MC156", "BBQ Plate", 399, 5, true, "Two meat combo", 500),
            new MainCourse("MC157", "Smoked Salmon", 429, 5, true, "Wood-fired fish", 450),
            new MainCourse("MC158", "BBQ Pizza", 349, 5, true, "With smoked meats", 480),
            new MainCourse("MC159", "BBQ Bowl", 299, 4, true, "Meat over rice", 400),
            new MainCourse("MC160", "BBQ Mac & Cheese", 259, 4, true, "With pulled pork", 450),
            
            new Dessert("DS071", "Banana Pudding", 149, 2, true, "Southern classic", 350),
            new Dessert("DS072", "Pecan Pie", 169, 3, true, "Nutty sweet pie", 400),
            new Dessert("DS073", "Bread Pudding", 159, 3, true, "With bourbon sauce", 380),
            new Dessert("DS074", "Cobbler", 139, 3, true, "Seasonal fruit", 300),
            new Dessert("DS075", "Fried Pie", 129, 2, true, "Handheld dessert", 250),
            new Dessert("DS076", "Sweet Potato Pie", 149, 3, true, "Southern specialty", 350),
            new Dessert("DS077", "Chocolate Chess Pie", 159, 2, true, "Rich chocolate", 400),
            new Dessert("DS078", "Peach Crisp", 139, 3, true, "Warm fruit dessert", 300),
            new Dessert("DS079", "S'mores", 119, 2, true, "Campfire classic", 250),
            new Dessert("DS080", "Fried Cheesecake", 179, 3, true, "Crispy outside", 450),
            
            new Beverage("BV141", "Sweet Tea", 79, 1, true, "Southern staple", 150),
            new Beverage("BV142", "Lemonade", 89, 1, true, "Fresh squeezed", 120),
            new Beverage("BV143", "Bourbon", 179, 1, true, "Kentucky straight", 100),
            new Beverage("BV144", "Root Beer", 69, 1, true, "Classic soda", 150),
            new Beverage("BV145", "Iced Coffee", 99, 1, true, "Cold brew", 50),
            new Beverage("BV146", "Mint Julep", 189, 2, true, "Bourbon cocktail", 200),
            new Beverage("BV147", "Peach Tea", 89, 1, true, "Fruit-infused", 120),
            new Beverage("BV148", "Hard Cider", 149, 1, true, "Local selection", 150),
            new Beverage("BV149", "Arnold Palmer", 99, 1, true, "Half tea, half lemonade", 120),
            new Beverage("BV150", "Sarsaparilla", 109, 1, true, "Old-fashioned soda", 140),
            new Beverage("BV151", "Bourbon Slush", 159, 2, true, "Frozen cocktail", 180),
            new Beverage("BV152", "Sweet Tea Vodka", 169, 1, true, "Southern cocktail", 150)
        ));
    }

    private void addFrenchMenu() {
        menu.addAll(Arrays.asList(
            new Starter("ST081", "Escargot", 299, 4, true, "Garlic butter snails", 250),
            new Starter("ST082", "French Onion Soup", 199, 3, true, "Caramelized onion soup", 300),
            new Starter("ST083", "Pâté", 229, 3, true, "Duck liver spread", 280),
            new Starter("ST084", "Brie en Croûte", 249, 4, true, "Baked brie in pastry", 350),
            new Starter("ST085", "Salade Niçoise", 219, 3, true, "Tuna salad", 280),
            new Starter("ST086", "Soupe à l'Oignon", 189, 3, true, "Classic onion soup", 250),
            new Starter("ST087", "Tartare de Boeuf", 279, 4, true, "Beef tartare", 220),
            new Starter("ST088", "Gougères", 179, 3, true, "Cheese puffs", 200),
            new Starter("ST089", "Ratatouille", 199, 4, true, "Vegetable stew", 180),
            new Starter("ST090", "Quiche Lorraine", 229, 4, true, "Savory custard pie", 300),
            
            new MainCourse("MC161", "Coq au Vin", 399, 6, true, "Chicken in wine", 450),
            new MainCourse("MC162", "Boeuf Bourguignon", 429, 7, true, "Beef stew", 500),
            new MainCourse("MC163", "Duck Confit", 449, 6, true, "Slow-cooked duck", 480),
            new MainCourse("MC164", "Bouillabaisse", 479, 7, true, "Seafood stew", 450),
            new MainCourse("MC165", "Cassoulet", 399, 6, true, "Bean and meat stew", 550),
            new MainCourse("MC166", "Steak Frites", 499, 5, true, "Steak with fries", 600),
            new MainCourse("MC167", "Croque Monsieur", 279, 4, true, "Ham and cheese sandwich", 400),
            new MainCourse("MC168", "Poulet Rôti", 349, 5, true, "Roast chicken", 450),
            new MainCourse("MC169", "Salmon en Papillote", 379, 5, true, "Steamed salmon", 400),
            new MainCourse("MC170", "Tarte Flambée", 299, 4, true, "Alsatian pizza", 380),
            new MainCourse("MC171", "Quenelles", 329, 5, true, "Fish dumplings", 350),
            new MainCourse("MC172", "Sole Meunière", 429, 5, true, "Butter-fried fish", 400),
            new MainCourse("MC173", "Confit de Canard", 449, 6, true, "Preserved duck", 480),
            new MainCourse("MC174", "Blanquette de Veau", 399, 6, true, "Veal stew", 450),
            new MainCourse("MC175", "Gigot d'Agneau", 499, 7, true, "Roast leg of lamb", 550),
            new MainCourse("MC176", "Truffle Pasta", 379, 5, true, "Luxury mushroom", 400),
            new MainCourse("MC177", "Moules Marinières", 349, 5, true, "Mussels in white wine", 350),
            new MainCourse("MC178", "Poulet Basquaise", 329, 5, true, "Chicken with peppers", 400),
            new MainCourse("MC179", "Pot-au-Feu", 399, 6, true, "Boiled meat and veg", 450),
            new MainCourse("MC180", "Choucroute Garnie", 379, 6, true, "Sauerkraut with meats", 500),
            
            new Dessert("DS081", "Crème Brûlée", 169, 3, true, "Burnt cream", 350),
            new Dessert("DS082", "Tarte Tatin", 179, 4, true, "Upside-down apple tart", 380),
            new Dessert("DS083", "Macarons", 129, 2, true, "Colorful meringue", 200),
            new Dessert("DS084", "Profiteroles", 159, 3, true, "Cream puffs", 300),
            new Dessert("DS085", "Mille-Feuille", 189, 3, true, "Napoleon pastry", 350),
            new Dessert("DS086", "Éclair", 139, 2, true, "Cream-filled pastry", 250),
            new Dessert("DS087", "Soufflé", 199, 4, true, "Fluffy baked dessert", 280),
            new Dessert("DS088", "Madeleines", 119, 2, true, "Shell-shaped cakes", 180),
            new Dessert("DS089", "Clafoutis", 149, 3, true, "Cherry custard", 300),
            new Dessert("DS090", "Pain Perdu", 139, 3, true, "French toast", 350),
            
            new Beverage("BV161", "Champagne", 599, 1, true, "French sparkling wine", 120),
            new Beverage("BV162", "Bordeaux", 399, 1, true, "Red wine", 120),
            new Beverage("BV163", "Chablis", 349, 1, true, "White wine", 110),
            new Beverage("BV164", "Kir Royale", 249, 2, true, "Champagne cocktail", 130),
            new Beverage("BV165", "Pastis", 179, 1, true, "Anise-flavored spirit", 100),
            new Beverage("BV166", "Cognac", 299, 1, true, "French brandy", 120),
            new Beverage("BV167", "Café au Lait", 99, 2, true, "Coffee with milk", 80),
            new Beverage("BV168", "Citron Pressé", 89, 1, true, "Fresh lemonade", 100),
            new Beverage("BV169", "Thé à la Menthe", 79, 1, true, "Mint tea", 5),
            new Beverage("BV170", "Vin Chaud", 129, 2, true, "Mulled wine", 120),
            new Beverage("BV171", "Perrier", 69, 1, true, "Sparkling water", 0),
            new Beverage("BV172", "Cidre", 149, 1, true, "French cider", 120)
        ));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCuisine() { return cuisine; }
    public double getDeliveryFee() { return deliveryFee; }
    public List<Meal> getMenu() { return menu; }
    public double getRating() { return rating; }
}

// ================== MENU MANAGER ==================
class MenuManager {
    private final List<Restaurant> restaurants = new ArrayList<>();
    
    public MenuManager() {
        // Initialize with sample restaurants
        restaurants.add(new Restaurant("R001", "Spice Trail", "Indian", 49));
        restaurants.add(new Restaurant("R002", "Pasta Paradise", "Italian", 59));
        restaurants.add(new Restaurant("R003", "Tokyo Grill", "Japanese", 69));
        restaurants.add(new Restaurant("R004", "Burger Barn", "American", 39));
        restaurants.add(new Restaurant("R005", "Fiesta Mexicana", "Mexican", 49));
        restaurants.add(new Restaurant("R006", "Dragon Palace", "Chinese", 59));
        restaurants.add(new Restaurant("R007", "Olive Grove", "Mediterranean", 49));
        restaurants.add(new Restaurant("R008", "Smokehouse", "BBQ", 59));
        restaurants.add(new Restaurant("R009", "Le Petit Bistro", "French", 79));
    }
    
    public List<Restaurant> getRestaurants() { return restaurants; }
}

// ================== CART SYSTEM ==================
class Cart {
    private final List<Meal> items = new ArrayList<>();
    
    public void addItem(Meal item) throws ItemUnavailableException {
        if (!item.isAvailable()) {
            throw new ItemUnavailableException(item.getName() + " is currently unavailable!");
        }
        items.add(item);
    }
    
    public void removeItem(Meal item) {
        items.remove(item);
    }
    
    public void clear() {
        items.clear();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public List<Meal> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public double getTotalPrice() {
        return items.stream().mapToDouble(Meal::getPrice).sum();
    }
}

class ItemUnavailableException extends Exception {
    public ItemUnavailableException(String message) {
        super(message);
    }
}

// ================== ORDER PROCESSING ==================
class Order {
    public enum OrderStatus {
        PREPARING("👨‍🍳 Preparing your order"),
        COOKING("🔥 Cooking in progress"),
        PACKAGING("📦 Packaging your meal"),
        READY("✅ Order ready for delivery"),
        DELIVERED("🛵 Order delivered!");
        
        private final String displayText;
        
        OrderStatus(String displayText) {
            this.displayText = displayText;
        }
        
        public String getDisplayText() {
            return displayText;
        }
    }
    
    private static int nextOrderId = 1000;
    private final int orderId;
    private final List<Meal> items;
    private final Restaurant restaurant;
    private OrderStatus status;
    private final Date orderTime;
    
    public Order(List<Meal> items, Restaurant restaurant) {
        this.orderId = nextOrderId++;
        this.items = new ArrayList<>(items);
        this.restaurant = restaurant;
        this.status = OrderStatus.PREPARING;
        this.orderTime = new Date();
    }
    
    public int getOrderId() { return orderId; }
    public List<Meal> getItems() { return Collections.unmodifiableList(items); }
    public Restaurant getRestaurant() { return restaurant; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Date getOrderTime() { return orderTime; }
    
    public double getTotalPrice() {
        return items.stream().mapToDouble(Meal::getPrice).sum() + restaurant.getDeliveryFee();
    }
}

class OrderProcessor extends Thread {
    public interface OrderUpdateListener {
        void onOrderStarted(Order order);
        void onOrderProgress(Order order, int progress);
        void onOrderCompleted(Order order);
    }
    
    private final Order order;
    private final OrderUpdateListener listener;
    
    public OrderProcessor(Order order, OrderUpdateListener listener) {
        this.order = order;
        this.listener = listener;
    }
    
    @Override
    public void run() {
        listener.onOrderStarted(order);
        
        // Simulate order processing
        for (int progress = 0; progress <= 100; progress += 10) {
            try {
                Thread.sleep(800); // Simulate time for each step
                
                // Update status at certain progress points
                if (progress == 30) {
                    order.setStatus(Order.OrderStatus.COOKING);
                } else if (progress == 60) {
                    order.setStatus(Order.OrderStatus.PACKAGING);
                } else if (progress == 90) {
                    order.setStatus(Order.OrderStatus.READY);
                }
                
                listener.onOrderProgress(order, progress);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        order.setStatus(Order.OrderStatus.DELIVERED);
        listener.onOrderCompleted(order);
    }
}