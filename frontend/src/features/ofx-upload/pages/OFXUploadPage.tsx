import { useNavigate } from 'react-router-dom';
import OFXUploader from '../components/OFXUploader';

const OFXUploadPage = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#f4f6f1] p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-emerald-800">
            Importar planilha ou OFX
          </h1>
          <div className="flex items-center space-x-3">
            <button
              onClick={() => navigate('/app')}
              className="btn btn-secondary"
            >
              Voltar ao Dashboard
            </button>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <OFXUploader />
        </div>
      </div>
    </div>
  );
};

export default OFXUploadPage;
