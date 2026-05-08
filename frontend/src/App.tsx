import { Routes, Route } from "react-router-dom";
import Header from "./components/Header";
import Footer from "./components/Footer";
import HomePage from "./pages/HomePage";
import ChatbotPage from "./pages/ChatbotPage";
import SourcesPage from "./pages/SourcesPage";

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/chatbot" element={<ChatbotPage />} />
        <Route path="/sources" element={<SourcesPage />} />
      </Routes>
      <Footer />
    </>
  );
}
