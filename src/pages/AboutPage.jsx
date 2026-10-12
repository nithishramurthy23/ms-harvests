import { Leaf, Truck, Users, Heart } from "lucide-react";

const features = [
  {
    icon: Leaf,
    title: "Organic Produce",
    description:
      "All our products are certified organic, grown without harmful pesticides or chemicals.",
  },
  {
    icon: Truck,
    title: "Fast Delivery",
    description:
      "Same-day delivery available for orders placed before 2 PM in selected areas.",
  },
  {
    icon: Users,
    title: "Local Farmers",
    description:
      "We partner with local farmers to bring you the freshest produce while supporting our community.",
  },
  {
    icon: Heart,
    title: "Quality Guarantee",
    description:
      "100% satisfaction guaranteed or your money back, no questions asked.",
  },
];

export default function AboutPage() {
  return (
    <div className="py-24 px-4">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold mb-6">About MS Fresh Harvest</h1>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto">
            We&apos;re passionate about bringing the freshest, organic produce from
            local farms directly to your table. Our mission is to make healthy
            eating accessible while supporting sustainable farming practices.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-16">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <div key={feature.title} className="text-center p-6">
                <div className="inline-block p-4 bg-green-100 rounded-full mb-4">
                  <Icon className="h-8 w-8 text-green-600" />
                </div>
                <h3 className="text-xl font-semibold mb-2">{feature.title}</h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            );
          })}
        </div>

        <div className="bg-green-50 rounded-2xl p-8 md:p-12">
          <div className="grid md:grid-cols-2 gap-8 items-center">
            <div>
              <h2 className="text-3xl font-bold mb-4">Our Story</h2>
              <p className="text-gray-600 mb-4">
                Founded in 2020, MS Fresh Harvest began with a simple idea:
                everyone deserves access to fresh, healthy produce. We started
                by partnering with three local farms and have since grown to
                work with over 50 farmers in the region.
              </p>
              <p className="text-gray-600">
                Today, we&apos;re proud to serve thousands of customers while
                maintaining our commitment to quality, sustainability, and
                supporting local agriculture.
              </p>
            </div>
            <div className="relative h-64 md:h-96">
              <img
                src="https://im.hunt.in/cg/Ooty/City-Guide/Carrot-Farming-in-Ooty.jpg"
                alt="Farm landscape"
                className="w-full h-full object-cover rounded-lg"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
