import { RouterProvider } from 'react-router';
import { router } from './routes';

export default function App() {
  return (
    <div className="bg-gray-100 min-h-screen flex justify-center items-center font-sans text-slate-900">
      <div className="w-full max-w-md bg-white h-[100dvh] relative overflow-hidden shadow-2xl sm:rounded-3xl sm:h-[850px] sm:my-8 flex flex-col">
        <RouterProvider router={router} />
      </div>
    </div>
  );
}
