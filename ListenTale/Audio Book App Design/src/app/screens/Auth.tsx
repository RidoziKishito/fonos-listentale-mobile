import React, { useState } from 'react';
import { useNavigate, Outlet } from 'react-router';
import { ArrowLeft, BookHeadphones, Mail, Lock, User, Calendar, Check, Target, ChevronRight } from 'lucide-react';

export function AuthLayout() {
  return (
    <div className="flex flex-col h-full bg-white relative overflow-y-auto">
      <Outlet />
    </div>
  );
}

export function Welcome() {
  const navigate = useNavigate();
  return (
    <div className="flex flex-col h-full p-6 items-center justify-center bg-violet-600 text-white relative">
      <div className="flex-1 flex flex-col items-center justify-center space-y-6">
        <div className="w-24 h-24 bg-white/20 rounded-3xl flex items-center justify-center backdrop-blur-sm">
          <BookHeadphones size={48} className="text-white" />
        </div>
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">ListenTale</h1>
          <p className="text-violet-200">Your favorite books, now in audio and text.</p>
        </div>
      </div>
      <div className="w-full space-y-4 pb-8">
        <button 
          onClick={() => navigate('/login')}
          className="w-full bg-white text-violet-600 font-semibold py-3.5 rounded-2xl shadow-sm active:scale-95 transition-transform"
        >
          Login
        </button>
        <button 
          onClick={() => navigate('/register')}
          className="w-full bg-violet-700 text-white font-semibold py-3.5 rounded-2xl active:scale-95 transition-transform"
        >
          Create an Account
        </button>
      </div>
    </div>
  );
}

export function Login() {
  const navigate = useNavigate();
  return (
    <div className="flex flex-col h-full p-6">
      <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-slate-800 self-start">
        <ArrowLeft size={24} />
      </button>
      <div className="mt-8 space-y-2">
        <h1 className="text-2xl font-bold text-slate-900">Welcome Back 👋</h1>
        <p className="text-slate-500">Sign in to continue listening</p>
      </div>
      
      <div className="mt-10 space-y-4">
        <div className="space-y-1">
          <label className="text-sm font-medium text-slate-700 ml-1">Email</label>
          <div className="relative">
            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
            <input type="email" placeholder="Enter your email" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition-all" />
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-sm font-medium text-slate-700 ml-1">Password</label>
          <div className="relative">
            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
            <input type="password" placeholder="Enter your password" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition-all" />
          </div>
        </div>
        <div className="flex justify-end">
          <button onClick={() => navigate('/forgot-password')} className="text-sm font-medium text-violet-600">Forgot Password?</button>
        </div>
      </div>

      <div className="mt-auto pb-8 pt-6">
        <button 
          onClick={() => navigate('/app')}
          className="w-full bg-violet-600 text-white font-semibold py-3.5 rounded-2xl shadow-sm active:scale-95 transition-transform shadow-violet-600/25"
        >
          Sign In
        </button>
        <p className="text-center mt-6 text-sm text-slate-600">
          Don't have an account? <button onClick={() => navigate('/register')} className="text-violet-600 font-semibold">Sign Up</button>
        </p>
      </div>
    </div>
  );
}

export function Register() {
  const navigate = useNavigate();
  return (
    <div className="flex flex-col h-full p-6">
      <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-slate-800 self-start">
        <ArrowLeft size={24} />
      </button>
      <div className="mt-8 space-y-2">
        <h1 className="text-2xl font-bold text-slate-900">Create Account ✨</h1>
        <p className="text-slate-500">Sign up to start your journey</p>
      </div>
      
      <div className="mt-8 space-y-4">
        <div className="relative">
          <User className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input type="text" placeholder="Full Name" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500" />
        </div>
        <div className="relative">
          <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input type="email" placeholder="Email" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500" />
        </div>
        <div className="relative">
          <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input type="password" placeholder="Password" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500" />
        </div>
      </div>

      <div className="mt-auto pb-8 pt-6">
        <button 
          onClick={() => navigate('/setup')}
          className="w-full bg-violet-600 text-white font-semibold py-3.5 rounded-2xl shadow-sm active:scale-95 transition-transform"
        >
          Continue
        </button>
        <p className="text-center mt-6 text-sm text-slate-600">
          Already have an account? <button onClick={() => navigate('/login')} className="text-violet-600 font-semibold">Sign In</button>
        </p>
      </div>
    </div>
  );
}

export function ForgotPassword() {
  const navigate = useNavigate();
  const [sent, setSent] = useState(false);

  return (
    <div className="flex flex-col h-full p-6">
      <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-slate-800 self-start">
        <ArrowLeft size={24} />
      </button>
      <div className="mt-8 space-y-2">
        <h1 className="text-2xl font-bold text-slate-900">Reset Password 🔒</h1>
        <p className="text-slate-500">Enter your email and we'll send you a link to reset your password.</p>
      </div>
      
      {!sent ? (
        <div className="mt-8 space-y-6">
          <div className="relative">
            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
            <input type="email" placeholder="Email" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500" />
          </div>
          <button 
            onClick={() => setSent(true)}
            className="w-full bg-violet-600 text-white font-semibold py-3.5 rounded-2xl shadow-sm active:scale-95 transition-transform"
          >
            Send Reset Link
          </button>
        </div>
      ) : (
        <div className="mt-8 flex flex-col items-center justify-center p-8 bg-slate-50 rounded-3xl border border-slate-100 text-center">
          <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
            <Check size={32} />
          </div>
          <h3 className="font-semibold text-lg">Check your email</h3>
          <p className="text-slate-500 text-sm mt-2">We've sent password recovery instructions to your email.</p>
          <button 
            onClick={() => navigate('/login')}
            className="mt-6 font-semibold text-violet-600"
          >
            Back to Login
          </button>
        </div>
      )}
    </div>
  );
}

export function Setup() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const totalSteps = 4;

  const interests = ['Fiction', 'Self Help', 'Business', 'History', 'Romance', 'Science', 'Fantasy', 'Thriller', 'Biography'];

  return (
    <div className="flex flex-col h-full p-6">
      <div className="flex items-center space-x-4 mb-8">
        <button onClick={() => step > 1 ? setStep(step - 1) : navigate(-1)} className="p-2 -ml-2 text-slate-800">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
          <div className="h-full bg-violet-600 transition-all duration-300" style={{ width: `${(step / totalSteps) * 100}%` }} />
        </div>
        <span className="text-sm font-medium text-slate-500">{step}/{totalSteps}</span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {step === 1 && (
          <div className="space-y-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 mb-2">What's your name?</h1>
              <p className="text-slate-500">We'll use this to personalize your experience.</p>
            </div>
            <div className="relative">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
              <input type="text" placeholder="Display Name" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500" />
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 mb-2">Birthday & Gender</h1>
              <p className="text-slate-500">Help us recommend better content.</p>
            </div>
            <div className="space-y-4">
              <div className="relative">
                <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
                <input type="date" className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 text-slate-700" />
              </div>
              <select className="w-full bg-slate-50 border border-slate-200 rounded-2xl py-3.5 px-4 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 appearance-none text-slate-700">
                <option value="">Select Gender</option>
                <option value="male">Male</option>
                <option value="female">Female</option>
                <option value="other">Other</option>
                <option value="prefer-not">Prefer not to say</option>
              </select>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 mb-2">Your Interests</h1>
              <p className="text-slate-500">Select at least 3 topics you love.</p>
            </div>
            <div className="flex flex-wrap gap-3">
              {interests.map(i => (
                <button key={i} className="px-5 py-2.5 rounded-full border border-slate-200 text-slate-600 font-medium active:bg-violet-50 focus:border-violet-600 focus:bg-violet-50 focus:text-violet-700 transition-colors">
                  {i}
                </button>
              ))}
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="space-y-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 mb-2">Set Your Goals</h1>
              <p className="text-slate-500">How much do you want to read/listen?</p>
            </div>
            <div className="space-y-4">
              <div className="p-5 border border-violet-200 bg-violet-50 rounded-2xl flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 bg-violet-100 rounded-full flex items-center justify-center text-violet-600">
                    <Target size={20} />
                  </div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Daily Goal</h4>
                    <p className="text-sm text-slate-500">30 mins / day</p>
                  </div>
                </div>
                <ChevronRight className="text-violet-400" />
              </div>
              <div className="p-5 border border-slate-200 rounded-2xl flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center text-slate-600">
                    <Target size={20} />
                  </div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Weekly Goal</h4>
                    <p className="text-sm text-slate-500">2 books / week</p>
                  </div>
                </div>
                <ChevronRight className="text-slate-400" />
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="mt-auto pt-6">
        <button 
          onClick={() => step < totalSteps ? setStep(step + 1) : navigate('/app')}
          className="w-full bg-violet-600 text-white font-semibold py-3.5 rounded-2xl shadow-sm active:scale-95 transition-transform"
        >
          {step < totalSteps ? 'Continue' : 'Get Started'}
        </button>
      </div>
    </div>
  );
}